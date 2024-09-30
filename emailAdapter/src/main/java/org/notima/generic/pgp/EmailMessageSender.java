package org.notima.generic.pgp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.notima.businessobjects.adapter.tools.MessageSender;
import org.notima.businessobjects.adapter.tools.exception.MessageSenderException;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.PublicKey;
import org.notima.generic.ifacebusinessobjects.KeyManager;

public abstract class EmailMessageSender implements MessageSender {

    protected String emailHost;
    protected String emailUser;
    protected String emailPass;
    protected String emailPort = "25";
    protected String emailName;
    protected File senderPublicKey;
    protected File senderPrivateKey;
    protected String senderPrivateKeyPassword;
    protected boolean attachPublicKey;
    
    protected MimeMessage theMessageToSend;
    
    protected KeyManager	keyManager;

    @Override
    public String getType() {
        return "email";
    }
    
    protected Session getMailSession(){
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
     * Initializes the message to send, sets the sender, recipient and subject.
     * Checks if the recipient has a key-file for encryption.
     * 
     * @param message
     * @throws AddressException
     * @throws MessagingException
     */
    protected void initMessageToSend(Message message) throws AddressException, MessagingException {
    	
    	theMessageToSend = new MimeMessage(getMailSession());
        InternetAddress fromAddr;
    	fromAddr = new InternetAddress(emailUser);
    	if (emailName!=null && emailName.trim().length()>0) {
        	try {
				fromAddr.setPersonal(emailName);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
    	}
        theMessageToSend.setFrom(fromAddr);
        theMessageToSend.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(message.getRecipient().getEmail()));
        theMessageToSend.setSubject(message.getSubject(), "utf-8");
        
        lookupRecipientPublicKey(message);
    	
    }

    protected void lookupRecipientPublicKey(Message message) {

        if (keyManager!=null) {
            if(message.getRecipientPublicKey() == null){
                PublicKey key = keyManager.get(message.getRecipient().getEmail());
                if (key!=null) {
                	File keyFile = new File (key.getKeyFileLocation());
                	message.setRecipientPublicKey(keyFile);
                }
            }
        }
    	
    }
    
    
    /**
     * Get an input stream from the senders private key file.
     * The file location is retrieved from the key manager
     * unless it has been overridden.
     * @return	A FileInputStream
     * @throws MessageSenderException
     */
    protected FileInputStream getSenderPrivateKeyInputStream() throws MessageSenderException {
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
     */
    protected FileInputStream getRecipientPublicKeyInputStream(Message message) throws MessageSenderException {
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

	public String getEmailName() {
		return emailName;
	}

	public void setEmailName(String emailName) {
		this.emailName = emailName;
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

	public KeyManager getKeyManager() {
		return keyManager;
	}

	public void setKeyManager(KeyManager keyManager) {
		this.keyManager = keyManager;
	}
	
}
