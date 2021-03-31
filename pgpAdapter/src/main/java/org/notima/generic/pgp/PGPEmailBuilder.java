package org.notima.generic.pgp;

import java.io.File;
import java.io.IOException;

import javax.mail.MessagingException;

/**
 * Helper class to help create a PGPEmail instance and set the property values in the right order.
 */
public class PGPEmailBuilder {
    private String emailHost;
    private String emailUser;
    private String emailPass;
    private String emailPort;
    private String body;
    private String subject;
    private String contentType;
    private String recipient;
    private PGPKeyManager keyManager;
    private File recipientPublicKey;
    private File senderPublicKey;
    private File senderPrivateKey;
    private String senderPrivateKeyPassword;
    private boolean attachPublicKey;
    private boolean encrypt;
    private boolean sign;

    /**
     * @param emailHost
     * The hostname of the smtp mail server.
     */
    public PGPEmailBuilder setEmailHost(String emailHost) {
        this.emailHost = emailHost;
        return this;
    }

    /**
     * @param emailUser
     * The user name of the sender on the mail server.
     */
    public PGPEmailBuilder setEmailUser(String emailUser) {
        this.emailUser = emailUser;
        return this;
    }

    /**
     * @param emailPass
     * The password for the email user on the mail server.
     */
    public PGPEmailBuilder setEmailPass(String emailPass) {
        this.emailPass = emailPass;
        return this;
    }

    /**
     * @param emailPort
     * The smtp port number of the mail server.
     */
    public PGPEmailBuilder setEmailPort(String emailPort) {
        this.emailPort = emailPort;
        return this;
    }

    /**
     * @param body
     * The email body in plain text.
     */
    public PGPEmailBuilder setBody(String body) {
        this.body = body;
        return this;
    }

    public PGPEmailBuilder setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * @param contentType
     * Content type of the mail body (e.g. "text/html;charset=utf-8").
     */
    public PGPEmailBuilder setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * @param recipient
     * Email address of the recipient.
     */
    public PGPEmailBuilder setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    /**
     * The key manager is used to retrieve pgp keys automatically.
     * This is optional if sender and recipient keys are set manually
     * or if the messasge is sent unencrypted and unsigned.
     * 
     * @param keyManager
     * The key manager to retrieve pgp keys from.
     */
    public PGPEmailBuilder setKeyManager(PGPKeyManager keyManager) {
        this.keyManager = keyManager;
        return this;
    }

    /**
     * This will override any keys found in the key manager.
     * This does not need to be set if a key for the recipient is
     * available from the key manager.
     * 
     * @param recipientPublicKey
     * The pgp key to use when encrypting the message.
     */
    public PGPEmailBuilder setRecipientPublicKey(File recipientPublicKey) {
        this.recipientPublicKey = recipientPublicKey;
        return this;
    }

    /**
     * This will override any keys found in the key manager.
     * This does not need to be set if a default sender key is
     * available from the key manager.
     * 
     * @param senderPublicKey
     * The pgp key to send as an attachment in order for the recipient
     * to verify a signature or send an ecnrypted reply.
     */
    public PGPEmailBuilder setSenderPublicKey(File senderPublicKey) {
        this.senderPublicKey = senderPublicKey;
        return this;
    }

    /**
     * This will override any keys found in the key manager.
     * This does not need to be set if a default sender key is
     * available from the key manager.
     * 
     * @param senderPrivateKey
     * The pgp key to use when signing the message.
     */
    public PGPEmailBuilder setSenderPrivateKey(File senderPrivateKey) {
        this.senderPrivateKey = senderPrivateKey;
        return this;
    } 
    
    /**
     * This will override any password found in the key manager.
     * This does not need to be set if a key for the recipient is
     * available from the key manager.
     * 
     * @param senderPrivateKeyPassword
     * @return
     */
    public PGPEmailBuilder setSenderPrivateKeyPassword(String senderPrivateKeyPassword) {
        this.senderPrivateKeyPassword = senderPrivateKeyPassword;
        return this;
    }

    /**
     * @param attachPublicKey
     * Should the public sender key be sent as a file attachment with
     * the email? This should be set to true if the email is signed
     * and the recipient might not have the key already.
     */
    public PGPEmailBuilder setAttachPublicKey(boolean attachPublicKey) {
        this.attachPublicKey = attachPublicKey;
        return this;
    }

    /**
     * @param encrypt
     * Should the email body be encrypted using the recipient public
     * key?
     */
    public PGPEmailBuilder setEncrypt(boolean encrypt){
        this.encrypt = encrypt;
        return this;
    }

    /**
     * @param sign
     * Should the email body be signed digitaly using the sender 
     * private key?
     */
    public PGPEmailBuilder setSign(boolean sign){
        this.sign = sign;
        return this;
    }

    public PGPEmail build() throws KeyNotFoundException, MessagingException, IOException {
        PGPEmail instance = new PGPEmail();
        instance.setEmailHost(emailHost);
        instance.setEmailUser(emailUser);
        instance.setEmailPassword(emailPass);
        instance.setEmailPort(emailPort);
        instance.setSubject(subject);
        instance.setContentType(contentType);
        instance.setRecipient(recipient);
        instance.setKeyManager(keyManager);
        instance.setRecipientPublicKey(recipientPublicKey);
        instance.setSenderPublicKey(senderPublicKey);
        instance.setSenderPrivateKey(senderPrivateKey);
        instance.setSenderPrivateKeyPassword(senderPrivateKeyPassword);
        instance.setBody(body, encrypt, sign);
        if(attachPublicKey)
            instance.attachSenderPublicKey();
        return instance;
    }
}
