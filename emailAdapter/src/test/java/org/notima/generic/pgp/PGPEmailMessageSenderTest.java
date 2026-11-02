package org.notima.generic.pgp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.notima.businessobjects.adapter.tools.exception.MessageSenderException;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.PublicKey;
import org.notima.generic.ifacebusinessobjects.KeyManager;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * Sends through an SMTP server (GreenMail) and checks what arrived: the PGP/MIME structure
 * on the raw bytes, signatures and decryption with Bouncy Castle and, if installed, with GnuPG.
 */
public class PGPEmailMessageSenderTest {

	private static final String SENDER = "sender@example.com";
	private static final String RECIPIENT = "recipient@example.com";
	private static final String SENDER_PASS = "sender passphrase";
	private static final String RECIPIENT_PASS = "recipient passphrase";
	/** What mail servers like to change: trailing spaces, long lines, "From " and "." lines, 8 bit */
	private static final String BODY = "<p>Hej Åsa,</p>   \nrad med blanksteg på slutet   \nFrom the start\n.\n"
			+ "lång rad " + String.join(" ", java.util.Collections.nCopies(40, "ord")) + "\nslut";
	/** Text in mail has CRLF line ends */
	private static final String BODY_CRLF = BODY.replace("\n", "\r\n");

	@TempDir
	File dir;

	private GreenMail smtp;
	private PGPEmailMessageSender sender;
	private File senderPublic, senderSecret, recipientPublic, recipientSecret;

	@BeforeEach
	void setUp() throws Exception {
		smtp = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
		smtp.start();
		smtp.setUser(SENDER, SENDER, "smtp password");

		senderPublic = new File(dir, "sender.pub.asc");
		senderSecret = new File(dir, "sender.sec.asc");
		recipientPublic = new File(dir, "recipient.pub.asc");
		recipientSecret = new File(dir, "recipient.sec.asc");
		generate("Sender <" + SENDER + ">", SENDER_PASS, senderPublic, senderSecret);
		generate("Recipient <" + RECIPIENT + ">", RECIPIENT_PASS, recipientPublic, recipientSecret);

		sender = new PGPEmailMessageSender();
		sender.setEmailHost("127.0.0.1");
		sender.setEmailPort(String.valueOf(smtp.getSmtp().getPort()));
		sender.setEmailUser(SENDER);
		sender.setEmailPass("smtp password");
		sender.setEmailName("Avsändare Åström");
		sender.setSenderPublicKey(senderPublic);
		sender.setSenderPrivateKey(senderSecret);
		sender.setSenderPrivateKeyPassword(SENDER_PASS);
	}

	@AfterEach
	void tearDown() {
		smtp.stop();
	}

	@Test
	void plain() throws Exception {
		sender.send(message(false, false), null, false);
		PGPTestUtil.Entity mail = receive();
		assertTrue(mail.type().match("text/html"));
		assertEquals(BODY_CRLF, new MimeBodyPart(new ByteArrayInputStream(mail.raw)).getContent());
	}

	@Test
	void signed() throws Exception {
		sender.send(message(true, false), null, false);
		PGPTestUtil.Entity content = checkSigned(receive());
		assertEquals(BODY_CRLF, parse(content.raw).getContent());
	}

	@Test
	void encrypted() throws Exception {
		Message message = message(false, true);
		message.setRecipientPublicKey(recipientPublic);
		sender.send(message, null, false);
		PGPTestUtil.Entity content = checkEncrypted(receive());
		assertEquals(BODY_CRLF, parse(content.raw).getContent());
	}

	@Test
	void signedAndEncryptedWithAttachments() throws Exception {
		File attachment = new File(dir, "fil med namn.pdf");
		byte[] bytes = new byte[5000];
		new java.util.Random(1).nextBytes(bytes);
		Files.write(attachment.toPath(), bytes);
		Message message = message(true, true);
		message.addAttachment(attachment);

		sender.send(message, keyManager(), true);	// the key comes from the key manager

		PGPTestUtil.Entity signed = checkEncrypted(receive());
		PGPTestUtil.Entity content = checkSigned(signed);
		MimeMultipart mixed = (MimeMultipart) parse(content.raw).getContent();
		assertEquals(3, mixed.getCount());
		assertEquals(BODY_CRLF, mixed.getBodyPart(0).getContent());
		assertEquals("fil med namn.pdf", mixed.getBodyPart(1).getFileName());
		assertArrayEquals(bytes, toBytes(mixed.getBodyPart(1).getInputStream()));
		assertTrue(mixed.getBodyPart(2).isMimeType("application/pgp-keys"));
		assertArrayEquals(Files.readAllBytes(senderPublic.toPath()), toBytes(mixed.getBodyPart(2).getInputStream()));
	}

	@Test
	void noRecipientKeyNothingSent() throws Exception {
		MessageSenderException e = assertThrows(MessageSenderException.class, () -> sender.send(message(true, true), null, false));
		assertTrue(e.getMessage().contains("no public key"), e.getMessage());
		assertEquals(0, smtp.getReceivedMessages().length);
	}

	@Test
	void wrongPassphraseNothingSent() throws Exception {
		sender.setSenderPrivateKeyPassword("wrong");
		MessageSenderException e = assertThrows(MessageSenderException.class, () -> sender.send(message(true, false), null, false));
		assertTrue(e.getMessage().contains("passphrase"), e.getMessage());
		assertEquals(0, smtp.getReceivedMessages().length);
	}

	/**
	 * GnuPG verifies and decrypts what was sent, and keys made by GnuPG (ed25519, cv25519)
	 * work for signing and encryption.
	 */
	@Test
	void gnupg() throws Exception {
		Assumptions.assumeTrue(gpgAvailable(), "gpg not installed");
		File home = Files.createDirectories(new File(dir, "gnupg").toPath()).toFile();
		try {
			gpg(home, "--quick-generate-key", "Gpg User <gpg@example.com>", "ed25519", "sign,cert", "never");
			String fpr = new String(gpg(home, "--list-keys", "--with-colons", "gpg@example.com"), StandardCharsets.US_ASCII)
					.split("\nfpr:::::::::")[1].split(":")[0];
			gpg(home, "--quick-add-key", fpr, "cv25519", "encr", "never");
			File gpgPublic = new File(dir, "gpg.pub.asc");
			File gpgSecret = new File(dir, "gpg.sec.asc");
			Files.write(gpgPublic.toPath(), gpg(home, "--export", "--armor", "gpg@example.com"));
			Files.write(gpgSecret.toPath(), gpg(home, "--export-secret-keys", "--armor", "gpg@example.com"));
			gpg(home, "--import", senderPublic.getPath());
			gpg(home, "--import", recipientSecret.getPath());	// with RECIPIENT_PASS, see gpg()

			// Signed and encrypted with our keys: GnuPG decrypts and verifies
			Message message = message(true, true);
			message.setRecipientPublicKey(recipientPublic);
			sender.send(message, null, false);
			checkWithGpg(home, receive(), RECIPIENT_PASS);

			// The GnuPG key as sender and as recipient
			sender.setSenderPublicKey(gpgPublic);
			sender.setSenderPrivateKey(gpgSecret);
			sender.setSenderPrivateKeyPassword(GPG_PASS);
			message = message(true, true);
			message.getRecipient().setEmail("gpg@example.com");
			message.setRecipientPublicKey(gpgPublic);
			smtp.setUser("gpg@example.com", "gpg@example.com", "x");
			smtp.purgeEmailFromAllMailboxes();
			sender.send(message, null, false);
			checkWithGpg(home, receive(), GPG_PASS);
		} finally {
			try {
				new ProcessBuilder("gpgconf", "--kill", "gpg-agent").inheritIO().redirectErrorStream(true)
						.directory(dir).start().waitFor(10, TimeUnit.SECONDS);
			} catch (IOException ignore) {
				// no gpgconf
			}
		}
	}

	// --- PGP/MIME checks on the raw message ---

	/** multipart/signed with a valid signature of the sender: the signed content */
	private PGPTestUtil.Entity checkSigned(PGPTestUtil.Entity entity) throws Exception {
		assertTrue(entity.type().match("multipart/signed"), entity.type().toString());
		assertEquals("application/pgp-signature", entity.type().getParameter("protocol"));
		assertEquals("pgp-sha256", entity.type().getParameter("micalg"));
		List<PGPTestUtil.Entity> parts = entity.parts();
		assertEquals(2, parts.size());
		assertTrue(parts.get(1).type().match("application/pgp-signature"));
		assertTrue(PGPTestUtil.verify(parts.get(0).raw, parts.get(1).body, sender.getSenderPublicKey()), "signature");
		return parts.get(0);
	}

	/** multipart/encrypted for the recipient: the decrypted entity */
	private PGPTestUtil.Entity checkEncrypted(PGPTestUtil.Entity entity) throws Exception {
		assertTrue(entity.type().match("multipart/encrypted"), entity.type().toString());
		assertEquals("application/pgp-encrypted", entity.type().getParameter("protocol"));
		List<PGPTestUtil.Entity> parts = entity.parts();
		assertEquals(2, parts.size());
		assertTrue(parts.get(0).type().match("application/pgp-encrypted"));
		assertTrue(parts.get(0).text().contains("Version: 1"));
		assertTrue(parts.get(1).type().match("application/octet-stream"));
		assertTrue(parts.get(1).text().startsWith("-----BEGIN PGP MESSAGE-----"));
		assertFalse(new String(entity.raw, StandardCharsets.ISO_8859_1).contains("rad med blanksteg"), "plain text in the encrypted mail");
		return new PGPTestUtil.Entity(PGPTestUtil.decrypt(parts.get(1).body, recipientSecret, RECIPIENT_PASS));
	}

	private void checkWithGpg(File home, PGPTestUtil.Entity mail, String passphrase) throws Exception {
		File encrypted = new File(dir, "encrypted.asc");
		Files.write(encrypted.toPath(), mail.parts().get(1).body);
		byte[] decrypted = gpg(home, "--pinentry-mode", "loopback", "--passphrase", passphrase, "--decrypt", encrypted.getPath());
		PGPTestUtil.Entity signed = new PGPTestUtil.Entity(decrypted);
		List<PGPTestUtil.Entity> parts = signed.parts();
		File data = new File(dir, "signed-data");
		File signature = new File(dir, "signature.asc");
		Files.write(data.toPath(), parts.get(0).raw);
		Files.write(signature.toPath(), parts.get(1).body);
		String status = new String(gpg(home, "--status-fd", "1", "--verify", signature.getPath(), data.getPath()), StandardCharsets.UTF_8);
		assertTrue(status.contains("[GNUPG:] GOODSIG"), status);
		assertEquals(BODY_CRLF, parse(parts.get(0).raw).getContent());
	}

	// --- Helpers ---

	private static final String GPG_PASS = "gpg passphrase";

	private static boolean gpgAvailable() {
		try {
			return new ProcessBuilder("gpg", "--version").redirectErrorStream(true).start().waitFor() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	/** Runs gpg in its own home (batch, the passphrase GPG_PASS for new keys): its output */
	private byte[] gpg(File home, String... args) throws Exception {
		List<String> command = new ArrayList<String>(Arrays.asList("gpg", "--homedir", home.getPath(), "--batch", "--yes"));
		if (args[0].startsWith("--quick") || args[0].equals("--export-secret-keys") || args[0].equals("--import")) {
			command.addAll(Arrays.asList("--pinentry-mode", "loopback", "--passphrase", GPG_PASS));
		}
		command.addAll(Arrays.asList(args));
		Process p = new ProcessBuilder(command).redirectError(new File(dir, "gpg.err")).start();
		byte[] out = toBytes(p.getInputStream());
		assertTrue(p.waitFor(60, TimeUnit.SECONDS), "gpg hangs");
		assertEquals(0, p.exitValue(), String.join(" ", args) + ": " + new String(Files.readAllBytes(new File(dir, "gpg.err").toPath()), StandardCharsets.UTF_8));
		return out;
	}

	private Message message(boolean signed, boolean encrypted) {
		Person recipient = new Person();
		recipient.setEmail(RECIPIENT);
		Message message = new Message();
		message.setRecipient(recipient);
		message.setSubject("Ämne med åäö");
		message.setBody(BODY);
		message.setContentType("text/html;charset=utf-8");
		message.setSigned(signed);
		message.setEncrypted(encrypted);
		return message;
	}

	private KeyManager keyManager() {
		return new KeyManager() {
			public PublicKey get(String userId) {
				if (!RECIPIENT.equals(userId)) {
					return null;
				}
				PublicKey key = new PublicKey();
				key.setUserId(userId);
				key.setKeyFileLocation(recipientPublic.getPath());
				return key;
			}
			public PublicKey add(PublicKey key) { return key; }
			public PublicKey update(PublicKey key) { return key; }
			public PublicKey remove(PublicKey key) { return key; }
			public List<PublicKey> list() { return new ArrayList<PublicKey>(); }
		};
	}

	/** The one received message, raw */
	private PGPTestUtil.Entity receive() throws Exception {
		assertTrue(smtp.waitForIncomingEmail(10000, 1));
		MimeMessage[] received = smtp.getReceivedMessages();
		assertEquals(1, received.length);
		assertEquals("Ämne med åäö", received[0].getSubject());
		ByteArrayOutputStream raw = new ByteArrayOutputStream();
		received[0].writeTo(raw);
		return new PGPTestUtil.Entity(raw.toByteArray());
	}

	private static MimeBodyPart parse(byte[] entity) throws Exception {
		return new MimeBodyPart(new ByteArrayInputStream(entity));
	}

	private static void generate(String userId, String passphrase, File publicFile, File secretFile) throws Exception {
		try (FileOutputStream pub = new FileOutputStream(publicFile); FileOutputStream sec = new FileOutputStream(secretFile)) {
			PGPKeys.generateKeyPair(userId, passphrase, pub, sec);
		}
	}

	private static byte[] toBytes(java.io.InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		for (int n; (n = in.read(buf)) > 0;) {
			out.write(buf, 0, n);
		}
		return out.toByteArray();
	}

}
