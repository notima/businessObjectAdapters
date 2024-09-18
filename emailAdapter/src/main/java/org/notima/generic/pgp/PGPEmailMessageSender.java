package org.notima.generic.pgp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.notima.businessobjects.adapter.tools.exception.MessageSenderException;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.exception.KeyNotFoundException;
import org.notima.generic.ifacebusinessobjects.KeyManager;

import me.sniggle.pgp.crypt.PGPMessageEncryptor;
import me.sniggle.pgp.crypt.PGPMessageSigner;

public class PGPEmailMessageSender extends EmailMessageSender {

    /**
     * Send the email to the recipient
     * @param message
     * The message to be sent
     * @param keyManager
     * the keyManager to retrieve public recipient keys from if no keys are provided in the message
     * @param attachSenderPublicKey
     * If set to true, The public sender key will be sent as an attachment.
     */
    public void send(Message message, KeyManager keyManager, boolean attachSenderPublicKey) throws MessageSenderException {
    	this.keyManager = keyManager;
        MimeMultipart emailContent = new MimeMultipart();
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        try {
        	initMessageToSend(message);
        	
        	if (!message.isEncrypted() && !message.isSigned()) {
        		messageBodyPart.setContent(message.getBody(), message.getContentType());
                emailContent.addBodyPart(messageBodyPart);

        	} else if (message.isEncrypted() && !message.isSigned()){
        		messageBodyPart.setContent(encryptMessageBody(message), message.getContentType());
                emailContent.addBodyPart(messageBodyPart);

        	} else if (message.isSigned() && !message.isEncrypted()) {
        		emailContent = signMessageBody(message);
        	}
            theMessageToSend.setContent(emailContent);

            if(attachSenderPublicKey){
                attachSenderPublicKey(emailContent);
            }

            if(message.getAttachemnts() != null){
                for(File attachment : message.getAttachemnts()){
                    attachFile(emailContent, attachment);
                }
            }

            Transport.send(theMessageToSend); 
        } catch (MessagingException e) {
            throw new MessageSenderException("Failed to send email message", e);
        }
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
     * @throws MessagingException 
     * @throws KeyNotFoundException
     */
    private MimeMultipart signMessageBody(Message message) throws MessageSenderException, MessagingException {

        MimeBodyPart theActualMessage = new MimeBodyPart();
        theActualMessage.setText(message.getBody());

        PGPMessageSigner signer = new PGPMessageSigner();
        
        ByteArrayInputStream messageIn = new ByteArrayInputStream(message.getBody().getBytes());
        ByteArrayOutputStream signedMessageOut = new ByteArrayOutputStream();

        signer.signMessage(
            getSenderPrivateKeyInputStream(), 
            message.getRecipient().getEmail(),
            senderPrivateKeyPassword, 
            messageIn, 
            signedMessageOut);
        
        MimeBodyPart signedPart = new MimeBodyPart();
        DataSource ds = new ByteArrayDataSource(signedMessageOut.toByteArray(), "application/pgp-signature");
        signedPart.setDataHandler(new DataHandler(ds));
        signedPart.setHeader("Content-Type", "application/pgp-signature; name=\"signature.asc\"");
        signedPart.setHeader("Content-Disposition", "inline; filename=\"signature.asc\"");

        // Create a Multipart to hold the message parts
        MimeMultipart multipart = new MimeMultipart();
        multipart.setSubType("signed; protocol=\"application/pgp-signature\"; micalg=php-sha256");
        multipart.addBodyPart(theActualMessage);
        multipart.addBodyPart(signedPart);
        
        return multipart;
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


}
