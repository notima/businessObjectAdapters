package org.notima.generic.pgp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.notima.businessobjects.adapter.tools.MessageSender;
import org.notima.businessobjects.adapter.tools.exception.MessageSenderException;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.PublicKey;
import org.notima.generic.ifacebusinessobjects.KeyManager;

import me.sniggle.pgp.crypt.PGPMessageEncryptor;
import me.sniggle.pgp.crypt.PGPMessageSigner;

public class PGPEmailMessageSender implements MessageSender {
    private String emailHost;
    private String emailUser;
    private String emailPass;
    private String emailPort = "25";
    private File senderPublicKey;
    private File senderPrivateKey;
    private String senderPrivateKeyPassword;

    @Override
    public String getType() {
        return "email";
    }

    /**
     * Send the email to the recipient
     * @throws MessageSenderException
     * @throws MessagingException
     * @throws AddressException         
     * @throws KeyNotFoundException
     */
    public void send(Message message, KeyManager keyManager, boolean attachSenderPublicKey) throws MessageSenderException {
        MimeMultipart emailContent = new MimeMultipart();
        MimeMessage mimeMessage = new MimeMessage(getMailSession());  
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        try {
            mimeMessage.setFrom(new InternetAddress(emailUser));
            mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(message.getRecipient().getEmail()));
            mimeMessage.setSubject(message.getSubject(), "utf-8");
            messageBodyPart.setContent(processBody(message, keyManager), message.getContentType());
            emailContent.addBodyPart(messageBodyPart);
            mimeMessage.setContent(emailContent);

            if(attachSenderPublicKey){
                attachSenderPublicKey(emailContent);
            }

            if(message.getAttachemnts() != null){
                for(File attachment : message.getAttachemnts()){
                    attachFile(emailContent, attachment);
                }
            }

            Transport.send(mimeMessage); 
        } catch (MessagingException e) {
            throw new MessageSenderException("Failed to send email message", e);
        }
    }

    /**
     * encrypt or sign or both
     * @param message
     * @return
     * @throws MessageSenderException
     * @throws KeyNotFoundException
     */
    private String processBody(Message message, KeyManager keyManager) throws MessageSenderException {
        String processedBody = message.getBody();
        if(message.isEncrypted() && message.getRecipientPublicKey() == null){
            PublicKey key = keyManager.get(message.getRecipient().getEmail());
            File keyFile = new File (key.getKeyFileLocation());
            message.setRecipientPublicKey(keyFile);
        }

        if(message.isEncrypted() && message.isSigned()){
            processedBody = encryptAndSignMessageBody(message);
        }
        else if(message.isEncrypted()){
            processedBody = encryptMessageBody(message);
        }
        else if(message.isSigned()){
            processedBody = signMessageBody(message);
        }
        return processedBody;
    }

    private Session getMailSession(){
        Properties properties = new Properties();

        if (emailHost==null) {
        	throw new NullPointerException("Missing property emailHost");
        }
        if (emailUser==null) {
        	throw new NullPointerException("Missing property emailUser");
        }
        
        properties.setProperty("mail.smtp.host", emailHost); 
        properties.setProperty("mail.smtp.port", emailPort == null ? "25" : emailPort); 
        
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {  
            protected PasswordAuthentication getPasswordAuthentication() {  
                   return new PasswordAuthentication(emailUser, emailPass);
            }  
        });

        return session;
    }

    /**
     * Encrypt a message using provided recipient public key
     * or a key retrieved from the key manager.
     * @param message
     * @return
     * @throws MessageSenderException
     * @throws KeyNotFoundException
     */
    private String encryptMessageBody(Message message) throws MessageSenderException {
        PGPMessageEncryptor encryptor = new PGPMessageEncryptor();

        ByteArrayInputStream bodyIS = new ByteArrayInputStream(message.getBody().getBytes());
        ByteArrayOutputStream bodyOS = new ByteArrayOutputStream();

        encryptor.encrypt(
            getRecipientPublicKeyInputStream(message), 
            message.getSubject(), 
            bodyIS, 
            bodyOS);

        return bodyOS.toString();
    }

    /**
     * Sign a message using provided sender private key
     * or a key retrieved from the key manager.
     * @param message
     * @return
     * @throws MessageSenderException
     * @throws KeyNotFoundException
     */
    private String signMessageBody(Message message) throws MessageSenderException {
        PGPMessageSigner signer = new PGPMessageSigner();

        ByteArrayInputStream bodyIS = new ByteArrayInputStream(message.getBody().getBytes());
        ByteArrayOutputStream bodyOS = new ByteArrayOutputStream();

        signer.signMessage(
            getSenderPrivateKeyInputStream(), 
            message.getRecipient().getEmail(),
            senderPrivateKeyPassword, 
            bodyIS, 
            bodyOS);

        return bodyOS.toString();
    }

    /**
     * Encrypt and sign a message using either provided keys
     * or keys retrieved from the key manager.
     * @param message Message to encrypt and sign.
     * @return result
     * @throws MessageSenderException
     * @throws KeyNotFoundException
     * @throws Exception
     */
    private String encryptAndSignMessageBody(Message message) throws MessageSenderException {
        PGPMessageEncryptor encryptor = new PGPMessageEncryptor();

        ByteArrayInputStream bodyIS = new ByteArrayInputStream(message.getBody().getBytes());
        ByteArrayOutputStream bodyOS = new ByteArrayOutputStream();

        encryptor.encrypt(
            getRecipientPublicKeyInputStream(message), 
            getSenderPrivateKeyInputStream(), 
            emailUser, 
            senderPrivateKeyPassword, 
            message.getSubject(), 
            bodyIS, 
            bodyOS);

        return bodyOS.toString();
    }

    /**
     * Attach the public key of the sender as a file attachemnt in
     * order for the recipient to be able to verify signatures and
     * send encrypted replies.
     * @throws MessageSenderException
     * @throws IOException
     * @throws MessagingException
     */
    private void attachSenderPublicKey(MimeMultipart emailContent) throws MessageSenderException {
        attachFile(emailContent, senderPublicKey);
    }

    /**
     * Add a file attachment body part to the email.
     * @param attachment file to attach
     * @throws MessageSenderException
     * @throws MessagingException
     * @throws IOException
     */
    private void attachFile(MimeMultipart emailContent, File attachment) throws MessageSenderException {
        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        if(attachment == null)
            throw new MessageSenderException("Tried to attach null");
        try {
            attachmentBodyPart.attachFile(attachment);
            emailContent.addBodyPart(attachmentBodyPart);
        } catch (Exception e) {
            throw new MessageSenderException(String.format("Could not attach %s to the email.", attachment.getAbsolutePath()));
        }
    }

    /**
     * Get an input stream from the senders private key file.
     * The file location is retrieved from the key manager
     * unless it has been overridden.
     * @return
     * @throws MessageSenderException
     * @throws KeyNotFoundException
     * @throws Exception
     */
    private FileInputStream getSenderPrivateKeyInputStream() throws MessageSenderException {
        File privateKeyFile = senderPrivateKey;
        MessageSenderException exception = new MessageSenderException("The email can not be signed because no private key has been provided.");
        try {
            if(privateKeyFile == null)
                throw exception;
            return new FileInputStream(privateKeyFile);
        } catch (FileNotFoundException e) {
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Get an input stream from the recipeints public key file.
     * The file location is retrieved from the key manager
     * unless it has been overridden.
     * @return
     * @throws MessageSenderException
     * @throws KeyNotFoundException
     */
    private FileInputStream getRecipientPublicKeyInputStream(Message message) throws MessageSenderException {
        File keyFile = message.getRecipientPublicKey();
        MessageSenderException exception = new MessageSenderException("The email can not be encrypted because no public key has been provided");
        try {
            if(keyFile == null)
                throw exception;
            return new FileInputStream(message.getRecipientPublicKey());
        } catch (FileNotFoundException e) {
            exception.initCause(e);
            throw exception;
        }
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
