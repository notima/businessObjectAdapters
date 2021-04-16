package org.notima.businessobjects.adapter.tools;

import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class MessageSenderFactoryImpl implements MessageSenderFactory {

    private BundleContext ctx;
	
	public void setBundleContext(BundleContext c) {
		ctx = c;
	}

    @Override
    public MessageSender getMessageSender(String type) {
        MessageSender reference = null;
        try {
            Collection<ServiceReference<MessageSender>> references = ctx.getServiceReferences(MessageSender.class, null);
            for(ServiceReference<MessageSender> messageSenderRef : references) {
                MessageSender messageSender = ctx.getService(messageSenderRef);
                if(messageSender.getType().equals(type)){
                    reference = messageSender;
                    break;
                }
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        return reference;
    }
    
}
