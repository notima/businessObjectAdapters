package org.notima.generic.pgp;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.notima.businessobjects.adapter.tools.MessageSender;
import org.notima.businessobjects.adapter.tools.exception.MessageSenderException;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.PublicKey;
import org.notima.generic.ifacebusinessobjects.KeyManager;

/**
 * Configuration (EmailProperties) and SMTP session of the email message senders.
 */
public abstract class EmailMessageSender implements MessageSender {

	/** Connect, read and write timeout of the SMTP connection, ms */
	private static final String SMTP_TIMEOUT = "60000";

    protected String emailHost;
    protected String emailUser;
    protected String emailPass;
    protected String emailPort = "25";
    protected String emailName;
    /** true: STARTTLS is required (typically port 587) */
    protected boolean emailStartTls;
    /** true: TLS from the start (SMTPS, typically port 465) */
    protected boolean emailSsl;
    protected File senderPublicKey;
    protected File senderPrivateKey;
    protected String senderPrivateKeyPassword;

    @Override
    public String getType() {
        return "email";
    }

    /**
     * An SMTP session. Authenticates as emailUser when emailPass is set.
     */
    protected Session getMailSession() throws MessageSenderException {
        if (isEmpty(emailHost)) {
        	throw new MessageSenderException("Missing property emailHost (EmailProperties)");
        }
        if (isEmpty(emailUser)) {
        	throw new MessageSenderException("Missing property emailUser (EmailProperties)");
        }
        String protocol = emailSsl ? "smtps" : "smtp";
        Properties properties = new Properties();
        properties.setProperty("mail.transport.protocol", protocol);
        properties.setProperty("mail." + protocol + ".host", emailHost.trim());
        properties.setProperty("mail." + protocol + ".port", isEmpty(emailPort) ? (emailSsl ? "465" : "25") : emailPort.trim());
        properties.setProperty("mail." + protocol + ".connectiontimeout", SMTP_TIMEOUT);
        properties.setProperty("mail." + protocol + ".timeout", SMTP_TIMEOUT);
        properties.setProperty("mail." + protocol + ".writetimeout", SMTP_TIMEOUT);
        if (emailStartTls) {
        	properties.setProperty("mail.smtp.starttls.enable", "true");
        	properties.setProperty("mail.smtp.starttls.required", "true");
        }
        if (emailStartTls || emailSsl) {
        	properties.setProperty("mail." + protocol + ".ssl.checkserveridentity", "true");
        }
        if (isEmpty(emailPass)) {
        	return Session.getInstance(properties);
        }
        properties.setProperty("mail." + protocol + ".auth", "true");
        return Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailUser.trim(), emailPass);
            }
        });
    }

    /**
     * A new message with sender, recipient, subject and date (no content).
     */
    protected MimeMessage newMimeMessage(Session session, Message message) throws MessagingException, MessageSenderException {
    	if (message.getRecipient() == null || isEmpty(message.getRecipient().getEmail())) {
    		throw new MessageSenderException("The message has no recipient email address");
    	}
    	MimeMessage mime = new MimeMessage(session);
        InternetAddress from = new InternetAddress(emailUser.trim());
    	if (!isEmpty(emailName)) {
        	try {
				from.setPersonal(emailName.trim(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new MessagingException("UTF-8 not supported", e);
			}
    	}
        mime.setFrom(from);
        mime.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(message.getRecipient().getEmail(), true));
        mime.setSubject(message.getSubject(), "UTF-8");
        mime.setSentDate(new java.util.Date());
        return mime;
    }

    /**
     * The recipient's public key file: the one of the message, else the one the key manager
     * has for the recipient's email address.
     */
    protected File lookupRecipientPublicKey(Message message, KeyManager keyManager) throws MessageSenderException {
    	if (message.getRecipientPublicKey() == null && keyManager != null && message.getRecipient() != null) {
    		PublicKey key = keyManager.get(message.getRecipient().getEmail());
    		if (key != null && !isEmpty(key.getKeyFileLocation())) {
    			message.setRecipientPublicKey(new File(key.getKeyFileLocation()));
    		}
    	}
    	File keyFile = message.getRecipientPublicKey();
    	if (keyFile == null) {
    		throw new MessageSenderException("The email can not be encrypted: no public key for the recipient");
    	}
    	if (!keyFile.canRead()) {
    		throw new MessageSenderException("The email can not be encrypted: can't read the recipient's public key " + keyFile);
    	}
    	return keyFile;
    }

    protected static boolean isEmpty(String s) {
    	return s == null || s.trim().isEmpty();
    }

	public String getEmailHost() {
		return emailHost;
	}

	public void setEmailHost(String emailHost) {
		this.emailHost = emailHost;
	}

	public String getEmailUser() {
		return emailUser;
	}

	public void setEmailUser(String emailUser) {
		this.emailUser = emailUser;
	}

	public String getEmailPass() {
		return emailPass;
	}

	public void setEmailPass(String emailPass) {
		this.emailPass = emailPass;
	}

	public String getEmailPort() {
		return emailPort;
	}

	public void setEmailPort(String emailPort) {
		this.emailPort = emailPort;
	}

	public String getEmailName() {
		return emailName;
	}

	public void setEmailName(String emailName) {
		this.emailName = emailName;
	}

	public boolean isEmailStartTls() {
		return emailStartTls;
	}

	public void setEmailStartTls(boolean emailStartTls) {
		this.emailStartTls = emailStartTls;
	}

	public boolean isEmailSsl() {
		return emailSsl;
	}

	public void setEmailSsl(boolean emailSsl) {
		this.emailSsl = emailSsl;
	}

	public File getSenderPublicKey() {
		return senderPublicKey;
	}

	public void setSenderPublicKey(File senderPublicKey) {
		this.senderPublicKey = senderPublicKey;
	}

	public File getSenderPrivateKey() {
		return senderPrivateKey;
	}

	public void setSenderPrivateKey(File senderPrivateKey) {
		this.senderPrivateKey = senderPrivateKey;
	}

	public String getSenderPrivateKeyPassword() {
		return senderPrivateKeyPassword;
	}

	public void setSenderPrivateKeyPassword(String senderPrivateKeyPassword) {
		this.senderPrivateKeyPassword = senderPrivateKeyPassword;
	}

}
