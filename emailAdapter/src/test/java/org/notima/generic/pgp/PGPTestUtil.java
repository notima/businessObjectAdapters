package org.notima.generic.pgp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;

import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.util.io.Streams;

/**
 * An independent reader of PGP/MIME (RFC 3156) for the tests: splits MIME entities on the raw
 * bytes (not with JavaMail), verifies and decrypts with Bouncy Castle.
 */
class PGPTestUtil {

	/** A MIME entity: its headers and the raw bytes of its body */
	static class Entity {
		final InternetHeaders headers;
		final byte[] body;
		final byte[] raw;
		Entity(byte[] raw) throws Exception {
			this.raw = raw;
			int split = indexOf(raw, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), 0);
			if (split < 0) {
				throw new IllegalArgumentException("No end of headers");
			}
			headers = new InternetHeaders(new ByteArrayInputStream(raw, 0, split + 4));
			byte[] b = new byte[raw.length - split - 4];
			System.arraycopy(raw, split + 4, b, 0, b.length);
			body = b;
		}
		ContentType type() throws Exception {
			String[] h = headers.getHeader("Content-Type");
			return new ContentType(h == null ? "text/plain" : h[0]);
		}
		/** The parts of a multipart, exactly as sent (RFC 2046: the CRLF before a delimiter belongs to it) */
		List<Entity> parts() throws Exception {
			String boundary = type().getParameter("boundary");
			byte[] delimiter = ("\r\n--" + boundary).getBytes(StandardCharsets.US_ASCII);
			byte[] withCrlf = new byte[body.length + 2];
			withCrlf[0] = '\r';
			withCrlf[1] = '\n';
			System.arraycopy(body, 0, withCrlf, 2, body.length);
			List<Entity> result = new ArrayList<Entity>();
			int pos = indexOf(withCrlf, delimiter, 0);
			while (pos >= 0) {
				int start = indexOf(withCrlf, "\r\n".getBytes(StandardCharsets.US_ASCII), pos + delimiter.length);
				if (withCrlf[pos + delimiter.length] == '-' && withCrlf[pos + delimiter.length + 1] == '-') {
					break;	// close delimiter
				}
				int next = indexOf(withCrlf, delimiter, start + 2);
				if (next < 0) {
					throw new IllegalArgumentException("No closing delimiter");
				}
				byte[] part = new byte[next - start - 2];
				System.arraycopy(withCrlf, start + 2, part, 0, part.length);
				result.add(new Entity(part));
				pos = next;
			}
			return result;
		}
		String text() {
			return new String(body, StandardCharsets.US_ASCII);
		}
	}

	static boolean verify(byte[] data, byte[] armoredSignature, File publicKeyFile) throws Exception {
		BcPGPObjectFactory factory = new BcPGPObjectFactory(PGPUtil.getDecoderStream(new ByteArrayInputStream(armoredSignature)));
		PGPSignature signature = ((PGPSignatureList) factory.nextObject()).get(0);
		PGPPublicKey key = publicKeys(publicKeyFile).getPublicKey(signature.getKeyID());
		if (key == null) {
			throw new IllegalArgumentException("Signed with another key");
		}
		signature.init(new BcPGPContentVerifierBuilderProvider(), key);
		signature.update(data);
		return signature.verify();
	}

	static byte[] decrypt(byte[] armored, File secretKeyFile, String passphrase) throws Exception {
		BcPGPObjectFactory factory = new BcPGPObjectFactory(PGPUtil.getDecoderStream(new ByteArrayInputStream(armored)));
		Object o = factory.nextObject();
		PGPEncryptedDataList list = (PGPEncryptedDataList) o;
		PGPSecretKeyRingCollection secrets;
		try (InputStream in = new FileInputStream(secretKeyFile)) {
			secrets = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator());
		}
		for (Iterator<?> it = list.getEncryptedDataObjects(); it.hasNext();) {
			PGPPublicKeyEncryptedData data = (PGPPublicKeyEncryptedData) it.next();
			PGPSecretKey secret = secrets.getSecretKey(data.getKeyID());
			if (secret == null) {
				continue;
			}
			PGPPrivateKey priv = secret.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(passphrase.toCharArray()));
			InputStream clear = data.getDataStream(new BcPublicKeyDataDecryptorFactory(priv));
			Object next = new BcPGPObjectFactory(clear).nextObject();
			if (next instanceof PGPCompressedData) {
				next = new BcPGPObjectFactory(((PGPCompressedData) next).getDataStream()).nextObject();
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Streams.pipeAll(((PGPLiteralData) next).getInputStream(), out);
			if (!data.isIntegrityProtected() || !data.verify()) {
				throw new IllegalStateException("Integrity check failed");
			}
			return out.toByteArray();
		}
		throw new IllegalArgumentException("Not encrypted to this key");
	}

	private static PGPPublicKeyRingCollection publicKeys(File file) throws Exception {
		try (InputStream in = new FileInputStream(file)) {
			return new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator());
		}
	}

	static int indexOf(byte[] data, byte[] pattern, int from) {
		outer:
		for (int i = Math.max(from, 0); i <= data.length - pattern.length; i++) {
			for (int j = 0; j < pattern.length; j++) {
				if (data[i + j] != pattern[j]) {
					continue outer;
				}
			}
			return i;
		}
		return -1;
	}

}
