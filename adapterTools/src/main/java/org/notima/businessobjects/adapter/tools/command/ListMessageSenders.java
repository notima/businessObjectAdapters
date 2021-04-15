package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.MessageSender;

@Command(scope = "notima", name = "list-message-senders", description = "Lists registered message senders")
@Service
public class ListMessageSenders implements Action {
    @Reference
	private Session sess;
	
	@Reference
	private List<MessageSender> senders;
	
	@Override
	public Object execute() throws Exception {
	
		if (senders==null) {
			System.out.println("No message senders registered");
		} else {
			for (MessageSender sender : senders) {

				sess.getConsole().println(sender.getClass().getName());
				
			}
			System.out.println(senders.size() + " message senders registered");
		}
		
		return null;
	}
}
