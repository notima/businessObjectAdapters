package org.notima.generic.pgp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import me.sniggle.pgp.crypt.PGPMessageEncryptor;
import me.sniggle.pgp.crypt.PGPMessageSigner;

public class PGPEmail {
    private String emailHost;
    private String emailUser;
    private String emailPass;
    private String emailPort = "25";
    private String body;
    private String subject;
    private String contentType = "text/plain;charset=utf-8";
    private String recipient;
    private PGPKeyManager keyManager;
    private File recipientPublicKey;
    private File senderPublicKey;
    private File senderPrivateKey;
    private String senderPrivateKeyPassword;

    private String processedBody;

    private MimeMultipart emailContent = new MimeMultipart();

    /**
     * Send the email to the recipient
     * @throws MessagingException
     * @throws AddressException
     */
    public void send() throws AddressException, MessagingException {
        MimeMessage message = new MimeMessage(getSession());  
        message.setFrom(new InternetAddress(emailUser)); 
        message.addRecipient(Message.RecipientType.TO,new InternetAddress(recipient));  
        message.setSubject(subject, "utf-8");

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(processedBody, contentType);

        emailContent.addBodyPart(messageBodyPart);

        message.setContent(emailContent);
        Transport.send(message);  
    }

    private Session getSession(){
        Properties properties = new Properties();

        if (emailHost==null) {
        	throw new NullPointerException("Missing property WebpayEmailProperties.emailHost");
        }
        if (emailUser==null) {
        	throw new NullPointerException("Missing property WebpayEmailProperties.emailUser");
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
     * @throws KeyNotFoundException
     */
    private String encryptMessage(String message) throws KeyNotFoundException {
        PGPMessageEncryptor encryptor = new PGPMessageEncryptor();

        ByteArrayInputStream bodyIS = new ByteArrayInputStream(message.getBytes());
        ByteArrayOutputStream bodyOS = new ByteArrayOutputStream();

        encryptor.encrypt(
            getRecipientPublicKeyInputStream(), 
            subject, 
            bodyIS, 
            bodyOS);

        return bodyOS.toString();
    }

    /**
     * Sign a message using provided sender private key
     * or a key retrieved from the key manager.
     * @param message
     * @return
     * @throws KeyNotFoundException
     */
    private String signMessage(String message) throws KeyNotFoundException {
        PGPMessageSigner signer = new PGPMessageSigner();

        ByteArrayInputStream bodyIS = new ByteArrayInputStream(message.getBytes());
        ByteArrayOutputStream bodyOS = new ByteArrayOutputStream();

        signer.signMessage(
            getSenderPrivateKeyInputStream(), 
            recipient,
            getSenderPrivateKeyPassword(), 
            bodyIS, 
            bodyOS);

        return bodyOS.toString();
    }

    /**
     * Encrypt and sign a message using either provided keys
     * or keys retrieved from the key manager.
     * @param message Message to encrypt and sign.
     * @return result
     * @throws KeyNotFoundException
     * @throws Exception
     */
    private String encryptAndSign(String message) throws KeyNotFoundException {
        PGPMessageEncryptor encryptor = new PGPMessageEncryptor();

        ByteArrayInputStream bodyIS = new ByteArrayInputStream(message.getBytes());
        ByteArrayOutputStream bodyOS = new ByteArrayOutputStream();

        encryptor.encrypt(
            getRecipientPublicKeyInputStream(), 
            getSenderPrivateKeyInputStream(), 
            emailUser, 
            getSenderPrivateKeyPassword(), 
            subject, 
            bodyIS, 
            bodyOS);

        return bodyOS.toString();
    }

    /**
     * Attach the public key of the sender as a file attachemnt in
     * order for the recipient to be able to verify signatures and
     * send encrypted replies.
     * @throws IOException
     * @throws MessagingException
     */
    public void attachSenderPublicKey() throws MessagingException, IOException {
        attachFile(getSenderPublicKey());
    }

    /**
     * Add a file attachment body part to the email.
     * @param attachment file to attach
     * @throws MessagingException
     * @throws IOException
     */
    public void attachFile(File attachment) throws MessagingException, IOException{
        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        attachmentBodyPart.attachFile(attachment);
        emailContent.addBodyPart(attachmentBodyPart);
    }

    /**
     * Get an input stream from the senders private key file.
     * The file location is retrieved from the key manager
     * unless it has been overridden.
     * @return
     * @throws KeyNotFoundException
     * @throws Exception
     */
    private FileInputStream getSenderPrivateKeyInputStream() throws KeyNotFoundException {
        File privateKeyFile = getSenderPrivateKey();
        KeyNotFoundException exception = new KeyNotFoundException("The email can not be signed because no private key could be found.");
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
     * @throws KeyNotFoundException
     */
    private FileInputStream getRecipientPublicKeyInputStream() throws KeyNotFoundException {
        File keyFile = getRecipientPublicKey();
        KeyNotFoundException exception = new KeyNotFoundException("The email can not be encrypted because no public key could be found for the user id: " + recipient);
        try {
            if(keyFile == null)
                throw exception;
            return new FileInputStream(getRecipientPublicKey());
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

    public String getEmailPassword() {
        return emailPass;
    }

    public void setEmailPassword(String emailPass) {
        this.emailPass = emailPass;
    }

    public String getEmailPort() {
        return emailPort;
    }

    public void setEmailPort(String emailPort) {
        if(emailPort != null)
            this.emailPort = emailPort;
    }

    /**
     * @return
     * The email body in plain text
     */
    public String getBody() {
        return body;
    }

    /**
     * Set the plain text email body and optionally encrypt and sign it.
     * @param body
     * The email body in plain text
     * @param encrypt
     * Should the email body be encrypted?
     * @param sign
     * Should the email body be signed?
     * @throws KeyNotFoundException
     */
    public void setBody(String body, boolean encrypt, boolean sign) throws KeyNotFoundException {
        this.body = body;
        if(encrypt && sign){
            processedBody = encryptAndSign(body);
        }else if(encrypt){
            processedBody = encryptMessage(body);
        }else if(sign){
            processedBody = signMessage(body);
        }else{
            processedBody = body;
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        if(contentType != null)
            this.contentType = contentType;
    }

    public String getRecipient(){
        return recipient;
    }

    public void setRecipient(String recipient){
        this.recipient = recipient;
    }

    public PGPKeyManager getKeyManager(){
        return keyManager;
    }

    public void setKeyManager(PGPKeyManager keyManager){
        this.keyManager = keyManager;
    }

    /**
     * Get the public key of the recipient.
     * 
     * @return
     * The provided recipient public key or a key from the key manager
     * if no key has been provided.
     */
    public File getRecipientPublicKey(){
        if(recipientPublicKey != null) {
            return recipientPublicKey;
        }else if(keyManager != null){
            File keyFile = null;
            PGPKey key = keyManager.get(getRecipient());
            if(key != null){
                keyFile = new File(key.getKeyFileLocation());
            }
            return keyFile;
        }
        return null;
    }

    /**
     * Override the defualt recipient public key from the key manager.
     * 
     * @param recipientPublicKey
     * public key file to use when encrypting message
     */
    public void setRecipientPublicKey(File recipientPublicKey) {
        this.recipientPublicKey = recipientPublicKey;
    }

    /**
     * @return
     * The public key of the sender. If no key has been provided,
     * an attempt will be made to retrieve a key from the key manager.
     */
    public File getSenderPublicKey() {
        if(senderPublicKey != null) {
            return senderPublicKey;
        }else if(keyManager != null){
            return keyManager.getSenderPublicKey();
        }
        return null;
    }

    /**
     * Override the defualt sender public key from the key manager.
     * 
     * @param senderPublicKey
     * public key file to use as public key of sender.
     */
    public void setSenderPublicKey(File senderPublicKey) {
        this.senderPublicKey = senderPublicKey;
    }

    /**
     * @return
     * The private key of the sender. If no key has been provided,
     * an attempt will be made to retrieve a key from the key manager.
     */
    public File getSenderPrivateKey() {
        if(senderPrivateKey != null) {
            return senderPrivateKey;
        }else if(keyManager != null){
            return keyManager.getSenderPrivateKey();
        }
        return null;
    }

    /**
     * Override the defualt sender private key from the key manager.
     * 
     * @param senderPrivateKey
     * private key file to use as private key of sender.
     */
    public void setSenderPrivateKey(File senderPrivateKey) {
        this.senderPrivateKey = senderPrivateKey;
    }

    /**
     * @return
     * The password of the private key of the sender. If no key has 
     * been provided, an attempt will be made to retrieve the password 
     * from the key manager.
     */
    public String getSenderPrivateKeyPassword() {
        if(senderPrivateKeyPassword != null) {
            return senderPrivateKeyPassword;
        }else if(keyManager != null){
            return keyManager.getSenderPrivateKeyPassword();
        }
        return null;
    }

    /**
     * Override the default sender private key password from the key 
     * manager.
     * 
     * @param senderPrivateKeyPassword
     */
    public void setSenderPrivateKeyPassword(String senderPrivateKeyPassword) {
        this.senderPrivateKeyPassword = senderPrivateKeyPassword;
    }
}
