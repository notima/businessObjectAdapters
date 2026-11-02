package org.notima.generic.pgp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.notima.businessobjects.adapter.tools.exception.MessageSenderException;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.ifacebusinessobjects.KeyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends email, optionally signed and/or encrypted with PGP/MIME (RFC 3156), which mail
 * programs such as Thunderbird, Outlook with gpg4o/GpgOL, Apple Mail with GPG Suite and
 * mutt verify and decrypt.
 *
 * <ul>
 * <li>signed: with the sender's private key (senderPrivateKey, senderPrivateKeyPassword)</li>
 * <li>encrypted: to the recipient's public key, the one of the message or else the one the
 * key manager has for the recipient's email address. Without a key nothing is sent.</li>
 * <li>signed and encrypted: signed, then encrypted. The subject isn't encrypted.</li>
 * </ul>
 *
 * Attachments of the message and (attachSenderPublicKey) the sender's public key are part of
 * the signed and encrypted content.
 */
public class PGPEmailMessageSender extends EmailMessageSender {

	private static final Logger log = LoggerFactory.getLogger(PGPEmailMessageSender.class);

    /**
     * Send the email to the recipient
     * @param message
     * The message to be sent
     * @param keyManager
     * the keyManager to retrieve public recipient keys from if no keys are provided in the message
     * @param attachSenderPublicKey
     * If set to true, The public sender key will be sent as an attachment.
     * @throws IOException
     */
	@Override
    public void send(Message message, KeyManager keyManager, boolean attachSenderPublicKey) throws MessageSenderException, IOException {
		Session session = getMailSession();
		MimeMessage mime = build(session, message, keyManager, attachSenderPublicKey);
		try {
			Transport.send(mime);
		} catch (MessagingException e) {
			throw new MessageSenderException("Failed to send email message: " + e.getMessage(), e);
		}
	}

	/**
	 * The message to send, signed and/or encrypted as the message asks.
	 */
	protected MimeMessage build(Session session, Message message, KeyManager keyManager, boolean attachSenderPublicKey) throws MessageSenderException, IOException {
		try {
			MimeMessage mime = newMimeMessage(session, message);
			if (!message.isSigned() && !message.isEncrypted()) {
				setContent(mime, message, attachSenderPublicKey);
				mime.saveChanges();
				return mime;
			}

			// Load the keys first: nothing is sent without them
			PGPKeys.SigningKey signingKey = message.isSigned() ? loadSigningKey() : null;
			PGPPublicKey recipientKey = message.isEncrypted() ? loadRecipientKey(message, keyManager) : null;

			MimeBodyPart content = PGPMime.newPart();
			setContent(content, message, attachSenderPublicKey);
			MimeMultipart result;
			if (signingKey != null) {
				result = PGPMime.sign(content, signingKey);
				if (recipientKey != null) {
					result = PGPMime.encrypt(PGPMime.asEntity(result), recipientKey);
				}
			} else {
				result = PGPMime.encrypt(content, recipientKey);
			}
			mime.setContent(result);
			mime.saveChanges();
			return mime;

		} catch (MessagingException e) {
			throw new MessageSenderException("Failed to create email message: " + e.getMessage(), e);
		} catch (PGPException e) {
			throw new MessageSenderException("PGP: " + e.getMessage(), e);
		}
	}

	/**
	 * The body (quoted-printable, so signatures survive mail servers), with the attachments
	 * and the sender's public key in a multipart/mixed if there are any.
	 */
	private void setContent(MimePart target, Message message, boolean attachSenderPublicKey) throws MessagingException, MessageSenderException {
		boolean attachKey = attachSenderPublicKey;
		if (attachKey && (senderPublicKey == null || !senderPublicKey.canRead())) {
			log.warn("The sender's public key can't be attached: senderPublicKey not set or not readable ({})", senderPublicKey);
			attachKey = false;
		}
		boolean hasAttachments = message.getAttachemnts() != null && !message.getAttachemnts().isEmpty();
		if (!hasAttachments && !attachKey) {
			setBody(target, message);
			return;
		}
		MimeMultipart mixed = new MimeMultipart("mixed");
		MimeBodyPart body = new MimeBodyPart();
		setBody(body, message);
		mixed.addBodyPart(body);
		if (hasAttachments) {
			for (File file : message.getAttachemnts()) {
				mixed.addBodyPart(attachment(file, null));
			}
		}
		if (attachKey) {
			mixed.addBodyPart(attachment(senderPublicKey, "application/pgp-keys"));
		}
		target.setContent(mixed);
	}

	private static void setBody(MimePart target, Message message) throws MessagingException {
		ContentType type = new ContentType(message.getContentType() == null ? "text/plain" : message.getContentType());
		if (type.getParameter("charset") == null) {
			type.setParameter("charset", "utf-8");
		}
		target.setContent(message.getBody() == null ? "" : message.getBody(), type.toString());
		target.setHeader("Content-Transfer-Encoding", "quoted-printable");
	}

	private static MimeBodyPart attachment(File file, String contentType) throws MessagingException, MessageSenderException {
		if (file == null || !file.canRead()) {
			throw new MessageSenderException("Can't attach " + file + " to the email: not readable");
		}
		MimeBodyPart part = new MimeBodyPart();
		FileDataSource source = new FileDataSource(file);
		part.setDataHandler(new DataHandler(source));
		part.setFileName(file.getName());
		part.setDisposition(MimeBodyPart.ATTACHMENT + "; filename=\"" + file.getName() + "\"");
		if (contentType != null) {
			part.setHeader("Content-Type", contentType + "; name=\"" + file.getName() + "\"");
		}
		part.setHeader("Content-Transfer-Encoding", "base64");
		return part;
	}

	private PGPKeys.SigningKey loadSigningKey() throws MessageSenderException, IOException, PGPException {
		if (senderPrivateKey == null || !senderPrivateKey.canRead()) {
			throw new MessageSenderException("The email can not be signed: senderPrivateKey not set or not readable (" + senderPrivateKey + ")");
		}
		try (InputStream in = new FileInputStream(senderPrivateKey)) {
			return PGPKeys.loadSigningKey(in, senderPrivateKeyPassword);
		}
	}

	private PGPPublicKey loadRecipientKey(Message message, KeyManager keyManager) throws MessageSenderException, IOException, PGPException {
		File keyFile = lookupRecipientPublicKey(message, keyManager);
		try (InputStream in = new FileInputStream(keyFile)) {
			return PGPKeys.loadEncryptionKey(in, message.getRecipient().getEmail());
		}
	}

}
