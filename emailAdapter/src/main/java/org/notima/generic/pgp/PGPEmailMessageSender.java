package org.notima.generic.pgp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
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
     * @throws IOException 
     */
    public void send(Message message, KeyManager keyManager, boolean attachSenderPublicKey) throws MessageSenderException, IOException {
    	this.keyManager = keyManager;
    	this.attachPublicKey = attachSenderPublicKey;
    	
        MimeMultipart emailContent = new MimeMultipart();
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        try {
        	initMessageToSend(message);
        	
        	if (!message.isEncrypted() && !message.isSigned()) {
        		messageBodyPart.setContent(message.getBody(), message.getContentType());
                emailContent.addBodyPart(messageBodyPart);
                theMessageToSend.setContent(emailContent);


        	} else if (message.isEncrypted() && !message.isSigned()){
        		messageBodyPart.setContent(encryptMessageBody(message), message.getContentType());
                emailContent.addBodyPart(messageBodyPart);
                theMessageToSend.setContent(emailContent);
                
        	} else if (message.isSigned() && !message.isEncrypted()) {
        		
        		MimeMultipart contentToSign = prepareForSigning(message, theMessageToSend);
        		emailContent = signMessage(theMessageToSend, contentToSign);
        		
        	} else if (message.isSigned() && message.isEncrypted()) {
        		
        		MimeMultipart signedContent = signMessageBody(message);
        		if (attachPublicKey) {
        			attachSenderPublicKey(signedContent);
        		}
        		theMessageToSend.setContent(signedContent);
        		MimeBodyPart encryptedPart = encryptMessage(message, theMessageToSend);
            	MimeBodyPart controlPart = new MimeBodyPart();
            	controlPart.setText("Version: 1");
            	controlPart.setHeader("Content-Type", "application/pgp-encrypted");

            	theMessageToSend = new MimeMessage(this.getMailSession());
            	initMessageToSend(message);
            	
                // Create a Multipart to hold the message parts
                MimeMultipart encryptedMultipart = new MimeMultipart();
                encryptedMultipart.setSubType("encrypted; protocol=\"application/pgp-encrypted\"; micalg=pgp-sha256");
            	encryptedMultipart.addBodyPart(controlPart);
            	encryptedMultipart.addBodyPart(encryptedPart);
            	emailContent = encryptedMultipart;
                theMessageToSend.setContent(emailContent);
        		
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

    private MimeBodyPart encryptMessage(Message canonicalMessage, MimeMessage mimeMessage) throws IOException, MessagingException, MessageSenderException {
    	
    	ByteArrayOutputStream mimeMessageBytes = new ByteArrayOutputStream();
    	mimeMessage.writeTo(mimeMessageBytes);
    	ByteArrayOutputStream cleansedMessage = removeLinesBeforeContentType(mimeMessageBytes);
    	
    	String encryptedMessage = encryptMessageBody(
    			canonicalMessage.getSubject(), 
    			cleansedMessage.toString(), 
    			canonicalMessage.getRecipientPublicKey());
    	
    	MimeBodyPart encryptedPart = new MimeBodyPart();
    	
        ByteArrayDataSource ds = new ByteArrayDataSource(encryptedMessage.getBytes(), "application/octet-stream");
    	encryptedPart.setDataHandler(new DataHandler(ds));    	
        encryptedPart.setHeader("Content-Type", "application/pgp-encrypted");
        encryptedPart.setHeader("Content-Disposition", "attachment; filename=\"encrypted.asc\"");

    	return encryptedPart;
    	
    }
    
    private MimeMultipart signMessage(MimeMessage mimeMessage, MimeMultipart contentToAddsignatureTo) throws IOException, MessagingException, MessageSenderException {
    	
    	ByteArrayOutputStream mimeMessageBytes = new ByteArrayOutputStream();
    	Enumeration<Header> headers = mimeMessage.getAllHeaders();
    	List<Header> headerList = new ArrayList<Header>();
    	for (Header h = headers.nextElement() ; headers.hasMoreElements() ; ) {
    		headerList.add(h);
    	}
    	
    	mimeMessage.writeTo(mimeMessageBytes);
    	ByteArrayOutputStream cleansedMessage = removeLinesBeforeContentType(mimeMessageBytes);
    	
    	// String withProtectedHeaders = addProtectedHeaders(mimeMessage, cleansedMessage.toString());
    	
    	String signature = signContent(mimeMessageBytes.toString());
    	
        MimeBodyPart signedPart = new MimeBodyPart();
        DataSource ds = new ByteArrayDataSource(signature.getBytes(), "application/pgp-signature");
        signedPart.setDataHandler(new DataHandler(ds));
        signedPart.setHeader("Content-Type", "application/pgp-signature; name=\"signature.asc\"");
        signedPart.setHeader("Content-Disposition", "inline; filename=\"signature.asc\"");

        contentToAddsignatureTo.addBodyPart(signedPart);
        
    	return contentToAddsignatureTo;
    	
    }
    
    private String getProtectedHeaders(MimeMessage mimeMessage) throws MessagingException {

    	String boundary = "----protectedHeaders";
    	
    	// Create a string to insert
    	String result = "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"; protected-headers=\"v1\"\n";
    	result += getProtectedHeadersAsString(mimeMessage);
    	
    	result += "\n" + boundary + "\n\n";
    	return result;
    }
    
    private String getProtectedHeadersAsString(MimeMessage mimeMessage) throws MessagingException {

    	String result = "From: " + mimeMessage.getHeader("From", null) + "\n";
    	result += "To: " + mimeMessage.getHeader("To", null) + "\n";
    	result += "Subject: " + mimeMessage.getHeader("Subject", null) + "\n\n";
    	
    	return result;
    	
    }
    
    private String addProtectedHeaders(MimeMessage mimeMessage, String content) throws MessagingException {
    	
    	return insertBeforeSecondContentType(content, getProtectedHeaders(mimeMessage));
    	
    }
    
    public static String insertBeforeSecondContentType(String input, String stringToInsert) {
        String searchStr = "Content-Type";
        int firstIndex = input.indexOf(searchStr);

        // Check if the first occurrence exists
        if (firstIndex == -1) {
            return input; // No "Content-Type" found
        }

        // Find the second occurrence of "Content-Type"
        int secondIndex = input.indexOf(searchStr, firstIndex + searchStr.length());
        
        // Check if the second occurrence exists
        if (secondIndex == -1) {
            return input; // Only one "Content-Type" found, no insertion needed
        }

        // Insert the new string before the second "Content-Type"
        StringBuilder result = new StringBuilder(input);
        result.insert(secondIndex, stringToInsert);

        return result.toString();
    }
    
    /**
     * Encrypt a message using provided recipient public key
     * or a key retrieved from the key manager.
     * @param message
     * @return
     * @throws MessageSenderException
     * @throws FileNotFoundException 
     * @throws KeyNotFoundException
     */
    private String encryptMessageBody(String messageSubject, String encryptString, File publicKeyFile) throws MessageSenderException, FileNotFoundException {
        PGPMessageEncryptor encryptor = new PGPMessageEncryptor();

        ByteArrayInputStream bodyIS = new ByteArrayInputStream(encryptString.getBytes());
        ByteArrayOutputStream bodyOS = new ByteArrayOutputStream();

        encryptor.encrypt(
            new FileInputStream(publicKeyFile), 
            messageSubject, 
            bodyIS, 
            bodyOS);

        return bodyOS.toString();
    }
    
    private MimeMultipart prepareForSigning(Message message, MimeMessage mimeMessage) throws MessagingException {

        // Create a Multipart to hold the message parts
        MimeMultipart multipartForSigning = new MimeMultipart();
        multipartForSigning.setSubType("signed; protocol=\"application/pgp-signature\"; micalg=pgp-sha1");
        // multipartForSigning.setPreamble(getProtectedHeadersAsString(mimeMessage));

        MimeBodyPart theActualMessage = new MimeBodyPart();
        theActualMessage.setContent(message.getBody(), message.getContentType());
        
        multipartForSigning.addBodyPart(theActualMessage);
        mimeMessage.setContent(multipartForSigning);
        
        return multipartForSigning;
    	
    }
    
    private String signContent(String contentToSign) throws MessageSenderException {
    	
        ByteArrayInputStream messageIn = new ByteArrayInputStream(contentToSign.getBytes());
        ByteArrayOutputStream signedMessageOut = new ByteArrayOutputStream();

        PGPMessageSigner signer = new PGPMessageSigner();
        
        signer.signMessage(
            getSenderPrivateKeyInputStream(), 
            getEmailUser(),
            senderPrivateKeyPassword, 
            messageIn, 
            signedMessageOut);
    	
        return signedMessageOut.toString();
    }
    
    /**
     * Sign a message using provided sender private key
     * or a key retrieved from the key manager.
     * @param message
     * @return
     * @throws MessageSenderException
     * @throws MessagingException 
     * @throws IOException 
     * @throws KeyNotFoundException
     */
    private MimeMultipart signMessageBody(Message message) throws MessageSenderException, MessagingException, IOException {

        MimeBodyPart theActualMessage = new MimeBodyPart();
        theActualMessage.setContent(message.getBody(), message.getContentType());

        MimeMultipart outerMultipart = new MimeMultipart("mixed");
        outerMultipart.addBodyPart(theActualMessage);
        theMessageToSend.setContent(outerMultipart);
        
        PGPMessageSigner signer = new PGPMessageSigner();
        
        ByteArrayOutputStream messageToSign = new ByteArrayOutputStream();
        outerMultipart.writeTo(messageToSign);
        ByteArrayInputStream messageIn = new ByteArrayInputStream(messageToSign.toByteArray());
        ByteArrayOutputStream signedMessageOut = new ByteArrayOutputStream();

        signer.signMessage(
            getSenderPrivateKeyInputStream(), 
            getEmailUser(),
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
        multipart.setSubType("signed; protocol=\"application/pgp-signature\"; micalg=pgp-sha256");
        multipart.addBodyPart(theActualMessage);
        multipart.addBodyPart(signedPart);
        
        return multipart;
    }

    /**
     * Attach the public key of the sender as a file attachemnt in
     * order for the recipient to be able to verify signatures and
     * send encrypted replies.
     * @throws MessageSenderException
     * @throws IOException
     * @throws MessagingException
     */
    private void attachSenderPublicKey(MimeMultipart emailContent) throws MessageSenderException, IOException, MessagingException {
    	MimeBodyPart attachmentBodyPart = new MimeBodyPart();
    	attachmentBodyPart.attachFile(senderPublicKey);
    	attachmentBodyPart.addHeader("Content-Type", "application/pgp-keys; name=\"" + senderPublicKey.getName() + "\"");
    	emailContent.addBodyPart(attachmentBodyPart);
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
     * Strip the first headers
     * 
     * @param input
     * @return
     * @throws IOException
     */
    private ByteArrayOutputStream removeLinesBeforeContentType(ByteArrayOutputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean foundContentType = false;
        
        // Convert ByteArrayOutputStream to BufferedReader to read line by line
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(input.toByteArray())));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
        
        String line;
        while ((line = reader.readLine()) != null) {
            if (!foundContentType) {
                if (line.toLowerCase().startsWith("content-type")) {
                    foundContentType = true;
                } else {
                    // Skip lines before the first occurrence of "Content-Type"
                    continue;
                }
            }
            // Write the remainder to the output stream
            writer.write(line);
            writer.newLine();
        }
        
        writer.flush();  // Ensure all data is written to the output
        return output;
    }    
    

}
