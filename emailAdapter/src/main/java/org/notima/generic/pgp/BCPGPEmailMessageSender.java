package org.notima.generic.pgp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.notima.businessobjects.adapter.tools.exception.MessageSenderException;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.PublicKey;
import org.notima.generic.ifacebusinessobjects.KeyManager;

public class BCPGPEmailMessageSender extends EmailMessageSender {

	private Message srcMessage;
	
	@Override
	public void send(Message message, KeyManager keyManager, boolean attachSenderPublicKey)
			throws MessageSenderException, IOException {
		this.keyManager = keyManager;
		
		srcMessage = message;
		try {
			initMessageToSend(srcMessage);
			
			if (srcMessage.isEncrypted()) {
				createEncryptedMessage();
			} else {
				createNormalMessage();
			}
			
			Transport.send(theMessageToSend); 
			
		} catch (Exception ee) {
			ee.printStackTrace();
		}
		
	}

	private void createNormalMessage() throws Exception {
		
        MimeMultipart emailContent = new MimeMultipart();
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(srcMessage.getBody(), srcMessage.getContentType());
        emailContent.addBodyPart(messageBodyPart);
        theMessageToSend.setContent(emailContent);
        
	}
	
	private void createEncryptedMessage() throws Exception {
		
		MimeBodyPart textPart = new MimeBodyPart();
		textPart.setText(srcMessage.getBody());
		
		// Sign the email body
		ByteArrayOutputStream signedOut = new ByteArrayOutputStream();
		PGPPublicKey senderPublicKey = getSenderPGPPublicKey();
		PGPPrivateKey senderPrivateKey = getSenderPGPPrivateKey();
		
//		signMimePart(textPart, senderPrivateKey, senderPublicKey, signedOut);
		
		// Encrypt the e-mail body
		ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
		PGPPublicKey recipientKey = getRecipientPublicKey();
		
		encryptMimePart(textPart, recipientKey, encryptedOut);

        // Create a multipart message
        MimeMultipart encryptedMultipart = new MimeMultipart();
        encryptedMultipart.setSubType("encrypted;\nprotocol=\"application/pgp-encrypted\"");
		
		// Add the "application/pgp-encrypted" part
        MimeBodyPart pgpEncryptedPart = new MimeBodyPart();
        pgpEncryptedPart.setText("Version: 1");
        pgpEncryptedPart.setHeader("Content-Type", "application/pgp-encrypted");
        pgpEncryptedPart.removeHeader("Content-Transfer-Encoding");
        encryptedMultipart.addBodyPart(pgpEncryptedPart);		
		
		// Create a new MimeBodyPart to hold the encrypted data
        MimeBodyPart encryptedPart = new MimeBodyPart();
        encryptedPart.setText(encryptedOut.toString());
        encryptedPart.setHeader("Content-Type", "application/octet-stream");
        encryptedPart.setFileName("encrypted.asc");
        encryptedPart.setDisposition("inline");

        encryptedMultipart.addBodyPart(encryptedPart);
        
        // Set the content of the message
        theMessageToSend.setContent(encryptedMultipart);		
		
	}
	
	private void encryptMimePart(MimeBodyPart textPart, PGPPublicKey encryptionKey, OutputStream armoredEncryptedOut) throws Exception {
        // Create the encrypted data generator with PGP encryption
        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256)
                        .setWithIntegrityPacket(true)
                        .setSecureRandom(new SecureRandom()));

        encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey));

        // Open the encrypted output stream
        ByteArrayOutputStream encryptedDataOut = new ByteArrayOutputStream();
        OutputStream pgpOut = encGen.open(encryptedDataOut, new byte[4096]);

        // Write the message content to the encrypted output stream
        ByteArrayOutputStream messageOut = new ByteArrayOutputStream();
        textPart.writeTo(messageOut);

        // Encrypt the message content
        pgpOut.write(messageOut.toByteArray());
        pgpOut.close();
        
        // Write ASCII-armored data to the final output stream
        try (ArmoredOutputStream armoredOut = new ArmoredOutputStream(armoredEncryptedOut)) {
            armoredOut.write(encryptedDataOut.toByteArray());
        }        
    }	
	
	
	  private void signMimePart(MimeBodyPart textPart, PGPPrivateKey privateKey, PGPPublicKey publicKey, OutputStream signedOut) throws Exception {
	        // Prepare the content to be signed
	        ByteArrayOutputStream contentOut = new ByteArrayOutputStream();
	        textPart.writeTo(contentOut);
	        byte[] contentBytes = contentOut.toByteArray();

	        // Signature generator setup
	        JcaPGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(publicKey.getAlgorithm(), PGPUtil.SHA256);
	        PGPSignatureGenerator sigGen = new PGPSignatureGenerator(signerBuilder);
	        sigGen.init(PGPSignature.BINARY_DOCUMENT, privateKey);

	        // Write the signed data to the output stream
	        ArmoredOutputStream armoredOut = new ArmoredOutputStream(signedOut);
	        sigGen.generateOnePassVersion(false).encode(armoredOut);
	        PGPLiteralDataGenerator literalDataGen = new PGPLiteralDataGenerator();
	        OutputStream literalOut = literalDataGen.open(armoredOut, PGPLiteralData.BINARY, "_CONSOLE", contentBytes.length, new java.util.Date());
	        literalOut.write(contentBytes);
	        sigGen.update(contentBytes);
	        sigGen.generate().encode(armoredOut);

	        literalOut.close();
	        armoredOut.close();
	    }	
	
	private PGPPublicKey getRecipientPublicKey() throws Exception {
		
        if(srcMessage.getRecipientPublicKey() == null){
            PublicKey key = keyManager.get(srcMessage.getRecipient().getEmail());
            File keyFile = new File (key.getKeyFileLocation());
            srcMessage.setRecipientPublicKey(keyFile);
        }
		
		PGPPublicKey key = loadPublicKey(this.getRecipientPublicKeyInputStream(srcMessage));
		return key;
		
	}
	
	private PGPPublicKey getSenderPGPPublicKey() throws Exception {
		
        if(srcMessage.getRecipientPublicKey() == null){
            PublicKey key = keyManager.get(srcMessage.getRecipient().getEmail());
            File keyFile = new File (key.getKeyFileLocation());
            srcMessage.setRecipientPublicKey(keyFile);
        }
		
		PGPPublicKey key = loadPublicKey(new FileInputStream(this.getSenderPublicKey()));
		return key;
		
	}
	
	private PGPPrivateKey getSenderPGPPrivateKey() throws Exception {
		return loadPrivateKey(new FileInputStream(senderPrivateKey), senderPrivateKeyPassword);
	}
	
	private PGPPrivateKey loadPrivateKey(InputStream privateKeyInputStream, String passphrase) throws Exception {
	        // PGPSecretKeyRingCollection parses the input stream to extract keys
	        PGPSecretKeyRingCollection keyRingCollection = new PGPSecretKeyRingCollection(
	                PGPUtil.getDecoderStream(privateKeyInputStream),
	                new JcaKeyFingerprintCalculator());

	        // Iterate over the key rings
	        Iterator<PGPSecretKeyRing> keyRingIterator = keyRingCollection.getKeyRings();
	        while (keyRingIterator.hasNext()) {
	            PGPSecretKeyRing keyRing = keyRingIterator.next();

	            // Iterate over the secret keys
	            Iterator<PGPSecretKey> secretKeyIterator = keyRing.getSecretKeys();
	            while (secretKeyIterator.hasNext()) {
	                PGPSecretKey secretKey = secretKeyIterator.next();

	                // Check if the key can be used for signing (i.e., it's a signing key)
	                if (secretKey.isSigningKey()) {
	                	// Decrypt the private key using the passphrase
	                    PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder().setProvider("BC")
	                            .build(passphrase.toCharArray());

	                    // Extract and return the private key
	                    return secretKey.extractPrivateKey(decryptor);
	                }
	            }
	        }
	        
        throw new IllegalArgumentException("No signing key found in the key ring.");
    }

	
	private PGPPublicKey loadPublicKey(InputStream keyIn) throws IOException, PGPException {
        // PGPUtil.getDecoderStream is used to handle both armored and binary key files
        InputStream decoderStream = PGPUtil.getDecoderStream(keyIn);
        
        // Create the PGPPublicKeyRingCollection from the decoder stream
        PGPPublicKeyRingCollection keyRingCollection = new PGPPublicKeyRingCollection(decoderStream, new JcaKeyFingerprintCalculator());

        // Iterate over the key rings and extract the first public key
        Iterator<PGPPublicKeyRing> keyRingIterator = keyRingCollection.getKeyRings();
        while (keyRingIterator.hasNext()) {
            PGPPublicKeyRing keyRing = keyRingIterator.next();
            Iterator<PGPPublicKey> publicKeyIterator = keyRing.getPublicKeys();

            while (publicKeyIterator.hasNext()) {
                PGPPublicKey publicKey = publicKeyIterator.next();

                // Ensure the public key can be used for encryption (check if it's an encryption key)
                if (publicKey.isEncryptionKey()) {
                    keyIn.close(); // Close the input stream after loading the key
                    return publicKey;  // Return the first available encryption key
                }
            }
        }

        keyIn.close();
        throw new IllegalArgumentException("No encryption key found in the file.");
    }	
	
}
