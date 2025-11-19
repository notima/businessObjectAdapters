package org.notima.businessobjects.adapter.tools.command;

import java.io.File;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.MessageSender;
import org.notima.businessobjects.adapter.tools.MessageSenderFactory;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.PublicKey;
import org.notima.generic.ifacebusinessobjects.KeyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

@Service
@Command(scope = "notima", name = "send-message", description = "Sends a message")
public class SendMessage extends AbstractAction {

	@Reference
    private ConfigurationAdmin configAdmin;

	@Reference
	private Session sess;

    @Option(name = "-k", aliases = { "--public-key" }, description = "Public PGP key of recipient for encryption. An attempt to find the key in the database will be made if this is omitted and the -e flag is present.", required = false, multiValued = false)
    private String pgpKey;

    @Option(name = "-a", aliases = { "--attach-public-key" }, description = "Attach the public key of the sender.", required = false, multiValued = false)
    private boolean attachKey;

    @Option(name = "-af", aliases = { "--attach-file" }, description = "Attach the file in the message.", required = false, multiValued = false)
    @Completion(FileCompleter.class)
    private String attachFile;
    
    @Option(name = "-e", aliases = { "--encrypt" }, description = "Send encrypted email.", required = false, multiValued = false)
    private boolean encrypt;

    @Option(name = "-s", aliases = { "--sign" }, description = "Digitally sign the email with public pgp key.", required = false, multiValued = false)
    private boolean sign;
    
    @Argument(index = 0, name = "subject", description = "The subject of the email.", required = true, multiValued = false)
	String subject;
    
    @Argument(index = 1, name = "body", description = "The body of the email.", required = true, multiValued = false)
	String emailBody;
    
    @Argument(index = 2, name = "recipient", description = "The email address to send the email to.", required = true, multiValued = false)
	String recipient;
	
    private File recipientPublicKey;
    private boolean attachPublicKey;
    
    private MessageSender emailSender;
    
    private KeyManager keyManager;
    
	@Override
	protected Object onExecute() throws Exception {
		initSender();
		sendEmail();
		return null;
	}

	private void initSender() throws Exception {
		
		emailSender = getMessageSenderFactory().getMessageSender("email");
		
		if (emailSender==null)
			throw new Exception("No message sender available");
		
	}
	
	private void sendEmail() throws Exception {
		
        Message message = new Message();
        Person recipientPerson = new Person();
        recipientPerson.setEmail(recipient);
        message.setBody(emailBody);
        message.setRecipient(recipientPerson);
        message.setSubject(subject);
        message.setEncrypted(encrypt);
        message.setSigned(sign);
        message.setRecipientPublicKey(recipientPublicKey);
        message.setContentType("text/html;charset=utf-8");
        
        if (attachFile!=null) {
        	message.addAttachment(new File(attachFile));
        }

        emailSender.send(message, getPGPService(), attachPublicKey);

        sess.getConsole().println(subject + " sent successfully to "+ recipient + (encrypt ? " using PGP-encryption" : ""));
		
	}
	
    public void privatizeIfPossible() {
    	
    	if (sign) {
    	
	    	if (keyManager!=null) {
	    		PublicKey pk = keyManager.get(recipient);
	    		if (pk!=null) {
	    			encrypt = true;
	    		}
	    	}
	    	
    	}
    	
    }
	
	
    protected ConfigurationAdmin getConfigAdmin() {
		if(configAdmin == null)
			configAdmin = (ConfigurationAdmin) getServiceReference(ConfigurationAdmin.class);
		return configAdmin;
	}

    private KeyManager getPGPService() {
		return (KeyManager) getServiceReference(KeyManager.class);
    }

    private MessageSenderFactory getMessageSenderFactory() {
        return (MessageSenderFactory) getServiceReference(MessageSenderFactory.class);
    }

	protected <S> Object getServiceReference(Class<S> clazz) {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		if (bundle != null) {
			BundleContext ctx = bundle.getBundleContext();
			ServiceReference<S> reference = ctx
					.getServiceReference(clazz);
			if (reference != null)
				return ctx.getService(reference);
		}
		return null;
    }

	
}
