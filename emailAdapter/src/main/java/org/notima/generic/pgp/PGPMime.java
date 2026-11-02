package org.notima.generic.pgp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Date;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

/**
 * PGP/MIME (RFC 3156) with Bouncy Castle and JavaMail.
 *
 * <ul>
 * <li>{@link #sign}: multipart/signed, the content entity and a detached signature (SHA-256)</li>
 * <li>{@link #encrypt}: multipart/encrypted, the encrypted content entity (AES-256, integrity protected)</li>
 * <li>signed and encrypted: the multipart/signed entity is encrypted (RFC 3156, 6.1)</li>
 * </ul>
 *
 * The signature covers the exact bytes of the content entity (headers and body, CRLF line ends).
 * The signed part is therefore built from those bytes, so JavaMail sends it unchanged.
 */
public class PGPMime {

	private static final String SIGNED_TYPE = "signed; micalg=pgp-sha256; protocol=\"application/pgp-signature\"";
	private static final String ENCRYPTED_TYPE = "encrypted; protocol=\"application/pgp-encrypted\"";

	private PGPMime() {
	}

	/**
	 * A body part whose headers (Content-Type, Content-Transfer-Encoding) can be
	 * completed before it is written on its own.
	 */
	private static class EntityPart extends MimeBodyPart {
		void completeHeaders() throws MessagingException {
			updateHeaders();
		}
	}

	/**
	 * A new body part for building a content entity.
	 */
	public static MimeBodyPart newPart() {
		return new EntityPart();
	}

	/**
	 * The entity as bytes in canonical form (CRLF line ends), as it will be sent.
	 */
	public static byte[] canonical(MimeBodyPart entity) throws MessagingException, IOException {
		if (entity instanceof EntityPart) {
			((EntityPart) entity).completeHeaders();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		entity.writeTo(out);
		return toCrlf(out.toByteArray());
	}

	/**
	 * multipart/signed of the content entity.
	 *
	 * @param content	the content entity, e.g. from {@link #newPart()}; text should be quoted-printable
	 * 					(trailing spaces and long lines don't survive mail servers unencoded)
	 */
	public static MimeMultipart sign(MimeBodyPart content, PGPKeys.SigningKey key) throws MessagingException, IOException, PGPException {
		byte[] data = canonical(content);

		PGPSignatureGenerator generator = new PGPSignatureGenerator(
				new BcPGPContentSignerBuilder(key.publicKey.getAlgorithm(), HashAlgorithmTags.SHA256));
		generator.init(PGPSignature.BINARY_DOCUMENT, key.privateKey);
		PGPSignatureSubpacketGenerator hashed = new PGPSignatureSubpacketGenerator();
		hashed.setSignatureCreationTime(false, new Date());
		hashed.setIssuerFingerprint(false, key.publicKey);
		generator.setHashedSubpackets(hashed.generate());
		generator.update(data);

		ByteArrayOutputStream signature = new ByteArrayOutputStream();
		ArmoredOutputStream armored = armored(signature);
		generator.generate().encode(new BCPGOutputStream(armored));
		armored.close();

		MimeMultipart signed = new MimeMultipart(SIGNED_TYPE);
		signed.addBodyPart(new MimeBodyPart(new ByteArrayInputStream(data)));
		MimeBodyPart signaturePart = new MimeBodyPart();
		signaturePart.setDataHandler(new DataHandler(new ByteArrayDataSource(signature.toByteArray(), "application/pgp-signature; name=\"signature.asc\"")));
		signaturePart.setHeader("Content-Description", "OpenPGP digital signature");
		signaturePart.setDisposition("attachment; filename=\"signature.asc\"");
		signaturePart.setHeader("Content-Transfer-Encoding", "7bit");
		signed.addBodyPart(signaturePart);
		return signed;
	}

	/**
	 * multipart/encrypted of an entity (a content entity, or a multipart/signed in a body part).
	 */
	public static MimeMultipart encrypt(MimeBodyPart entity, PGPPublicKey recipientKey) throws MessagingException, IOException, PGPException {
		byte[] data = canonical(entity);

		PGPEncryptedDataGenerator encryptor = new PGPEncryptedDataGenerator(
				new BcPGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
						.setWithIntegrityPacket(true)
						.setSecureRandom(new SecureRandom()));
		encryptor.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(recipientKey));

		ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
		ArmoredOutputStream armored = armored(encrypted);
		OutputStream encryptedOut = encryptor.open(armored, new byte[1 << 16]);
		PGPCompressedDataGenerator compressor = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZLIB);
		OutputStream compressedOut = compressor.open(encryptedOut);
		OutputStream literalOut = new PGPLiteralDataGenerator().open(compressedOut, PGPLiteralData.BINARY, "", data.length, new Date());
		literalOut.write(data);
		literalOut.close();
		compressor.close();
		encryptedOut.close();
		armored.close();

		MimeMultipart result = new MimeMultipart(ENCRYPTED_TYPE);
		MimeBodyPart control = new MimeBodyPart();
		control.setDataHandler(new DataHandler(new ByteArrayDataSource("Version: 1\r\n".getBytes("US-ASCII"), "application/pgp-encrypted")));
		control.setHeader("Content-Description", "PGP/MIME version identification");
		control.setHeader("Content-Transfer-Encoding", "7bit");
		result.addBodyPart(control);
		MimeBodyPart payload = new MimeBodyPart();
		payload.setDataHandler(new DataHandler(new ByteArrayDataSource(encrypted.toByteArray(), "application/octet-stream; name=\"encrypted.asc\"")));
		payload.setHeader("Content-Description", "OpenPGP encrypted message");
		payload.setDisposition("inline; filename=\"encrypted.asc\"");
		payload.setHeader("Content-Transfer-Encoding", "7bit");
		result.addBodyPart(payload);
		return result;
	}

	/**
	 * A multipart (e.g. from {@link #sign}) as an entity, to encrypt it.
	 */
	public static MimeBodyPart asEntity(MimeMultipart multipart) throws MessagingException {
		MimeBodyPart part = newPart();
		part.setContent(multipart);
		return part;
	}

	private static ArmoredOutputStream armored(OutputStream out) {
		ArmoredOutputStream armored = new ArmoredOutputStream(out);
		armored.clearHeaders();	// no "Version: BCPG ..." line
		return armored;
	}

	/**
	 * Converts bare LF (and bare CR) to CRLF.
	 */
	static byte[] toCrlf(byte[] in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(in.length + 64);
		for (int i = 0; i < in.length; i++) {
			byte b = in[i];
			if (b == '\r') {
				out.write('\r');
				out.write('\n');
				if (i + 1 < in.length && in[i + 1] == '\n') {
					i++;
				}
			} else if (b == '\n') {
				out.write('\r');
				out.write('\n');
			} else {
				out.write(b);
			}
		}
		return out.toByteArray();
	}

}
