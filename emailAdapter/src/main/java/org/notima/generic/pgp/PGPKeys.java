package org.notima.generic.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;

/**
 * Loading and generating OpenPGP keys with the lightweight API of Bouncy Castle
 * (no JCE provider has to be registered, which keeps the bundle self-contained in OSGi).
 *
 * Key files may be ASCII-armored or binary, as exported by GnuPG
 * (gpg --export --armor, gpg --export-secret-keys --armor).
 */
public class PGPKeys {

	/**
	 * A signing key with its decrypted private part.
	 */
	public static class SigningKey {
		public final PGPPublicKey publicKey;
		public final PGPPrivateKey privateKey;
		SigningKey(PGPPublicKey publicKey, PGPPrivateKey privateKey) {
			this.publicKey = publicKey;
			this.privateKey = privateKey;
		}
	}

	private PGPKeys() {
	}

	/**
	 * Finds the key to encrypt to: a valid (not revoked, not expired) encryption key, subkeys preferred.
	 * If the file holds several key rings, the one with a user id containing the email address is used
	 * (else the first one).
	 *
	 * @param keyIn		the public key file
	 * @param email		the recipient's email address, may be null
	 */
	public static PGPPublicKey loadEncryptionKey(InputStream keyIn, String email) throws IOException, PGPException {
		PGPPublicKeyRingCollection rings = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn), new BcKeyFingerprintCalculator());
		List<PGPPublicKeyRing> candidates = new ArrayList<PGPPublicKeyRing>();
		Iterator<PGPPublicKeyRing> it = rings.getKeyRings();
		while (it.hasNext()) {
			PGPPublicKeyRing ring = it.next();
			if (email != null && hasUserId(ring.getPublicKey(), email)) {
				candidates.add(0, ring);
			} else {
				candidates.add(ring);
			}
		}
		for (PGPPublicKeyRing ring : candidates) {
			PGPPublicKey found = null;
			Iterator<PGPPublicKey> keys = ring.getPublicKeys();
			while (keys.hasNext()) {
				PGPPublicKey key = keys.next();
				if (key.isEncryptionKey() && isValid(key) && hasFlags(key, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) {
					if (found == null || found.isMasterKey()) {
						found = key;	// the last valid subkey, else the primary key
					}
				}
			}
			if (found != null) {
				return found;
			}
		}
		throw new PGPException("No valid encryption key in the public key file" + (email != null ? " (" + email + ")" : ""));
	}

	/**
	 * Finds a key to sign with in a secret key file and decrypts it with the passphrase.
	 *
	 * @param keyIn			the secret key file
	 * @param passphrase	null or empty for an unprotected key
	 */
	public static SigningKey loadSigningKey(InputStream keyIn, String passphrase) throws IOException, PGPException {
		PGPSecretKeyRingCollection rings = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn), new BcKeyFingerprintCalculator());
		char[] pass = passphrase == null ? new char[0] : passphrase.toCharArray();
		Iterator<PGPSecretKeyRing> it = rings.getKeyRings();
		while (it.hasNext()) {
			Iterator<PGPSecretKey> keys = it.next().getSecretKeys();
			while (keys.hasNext()) {
				PGPSecretKey key = keys.next();
				PGPPublicKey pub = key.getPublicKey();
				// GnuPG stubs (gpg --export-secret-subkeys) have no private part
				if (!key.isSigningKey() || key.isPrivateKeyEmpty() || !isValid(pub) || !hasFlags(pub, KeyFlags.SIGN_DATA)) {
					continue;
				}
				PGPPrivateKey priv;
				try {
					priv = key.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass));
				} catch (PGPException e) {
					throw new PGPException("Can't unlock the private key: wrong passphrase?", e);
				}
				return new SigningKey(pub, priv);
			}
		}
		throw new PGPException("No usable signing key in the private key file");
	}

	/**
	 * Generates a key pair: an RSA primary key for certification and signing, and an RSA subkey
	 * for encryption, the secret keys protected by the passphrase (AES-256). Both are written
	 * ASCII-armored.
	 */
	public static void generateKeyPair(String userId, String passphrase, OutputStream publicOut, OutputStream secretOut) throws IOException, PGPException {
		if (userId == null || userId.trim().isEmpty()) {
			throw new IllegalArgumentException("A user id (email address) is required");
		}
		Date now = new Date();
		RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
		gen.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), new SecureRandom(), 3072, 80));
		BcPGPKeyPair primary = new BcPGPKeyPair(PublicKeyAlgorithmTags.RSA_GENERAL, gen.generateKeyPair(), now);
		BcPGPKeyPair encryption = new BcPGPKeyPair(PublicKeyAlgorithmTags.RSA_GENERAL, gen.generateKeyPair(), now);

		PGPSignatureSubpacketGenerator primaryAttrs = new PGPSignatureSubpacketGenerator();
		primaryAttrs.setKeyFlags(false, KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA);
		primaryAttrs.setPreferredSymmetricAlgorithms(false, new int[] { SymmetricKeyAlgorithmTags.AES_256, SymmetricKeyAlgorithmTags.AES_128 });
		primaryAttrs.setPreferredHashAlgorithms(false, new int[] { HashAlgorithmTags.SHA512, HashAlgorithmTags.SHA256 });
		primaryAttrs.setPreferredCompressionAlgorithms(false, new int[] { CompressionAlgorithmTags.ZLIB, CompressionAlgorithmTags.ZIP, CompressionAlgorithmTags.UNCOMPRESSED });
		primaryAttrs.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);
		PGPSignatureSubpacketGenerator encryptionAttrs = new PGPSignatureSubpacketGenerator();
		encryptionAttrs.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);

		PGPDigestCalculator sha1 = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
		char[] pass = passphrase == null ? new char[0] : passphrase.toCharArray();
		PGPKeyRingGenerator rings = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, primary, userId.trim(), sha1,
				primaryAttrs.generate(), null,
				new BcPGPContentSignerBuilder(PublicKeyAlgorithmTags.RSA_GENERAL, HashAlgorithmTags.SHA256),
				pass.length == 0 ? null : new BcPBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, sha1).build(pass));
		rings.addSubKey(encryption, encryptionAttrs.generate(), null);

		writeArmored(rings.generatePublicKeyRing().getEncoded(), publicOut);
		writeArmored(rings.generateSecretKeyRing().getEncoded(), secretOut);
	}

	private static void writeArmored(byte[] data, OutputStream out) throws IOException {
		ArmoredOutputStream armored = new ArmoredOutputStream(out);
		armored.clearHeaders();
		armored.write(data);
		armored.close();
	}

	private static boolean hasUserId(PGPPublicKey key, String email) {
		String wanted = email.trim().toLowerCase(Locale.ROOT);
		Iterator<String> ids = key.getUserIDs();
		while (ids.hasNext()) {
			if (ids.next().toLowerCase(Locale.ROOT).contains(wanted)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isValid(PGPPublicKey key) {
		if (key.hasRevocation()) {
			return false;
		}
		long valid = key.getValidSeconds();
		return valid == 0 || key.getCreationTime().getTime() + valid * 1000 > System.currentTimeMillis();
	}

	/**
	 * Whether the key's self-signature or binding signature allows any of the flags.
	 * Keys without key flags (old keys) are judged by their algorithm only.
	 */
	private static boolean hasFlags(PGPPublicKey key, int flags) {
		Iterator<PGPSignature> sigs = key.getSignatures();
		boolean anyFlags = false;
		while (sigs.hasNext()) {
			PGPSignatureSubpacketVector hashed = sigs.next().getHashedSubPackets();
			if (hashed != null && hashed.hasSubpacket(org.bouncycastle.bcpg.SignatureSubpacketTags.KEY_FLAGS)) {
				anyFlags = true;
				if ((hashed.getKeyFlags() & flags) != 0) {
					return true;
				}
			}
		}
		return !anyFlags;
	}

}
