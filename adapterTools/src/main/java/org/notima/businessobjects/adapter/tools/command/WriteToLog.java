package org.notima.businessobjects.adapter.tools.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Command(scope = "notima", name = "write-to-log", description = "Test method for logging")
@Service
public class WriteToLog extends AbstractAction {

	private static final Logger log = LoggerFactory.getLogger(WriteToLog.class);
	
	@Argument(index = 0, name = "message", description ="Message to log", required = true, multiValued = false)
	private String message = "";
	
	@Override
	protected Object onExecute() throws Exception {

		log.info(message);
		
		sess.getConsole().println("\"" + message + "\"" + " written to logger.");
		
		return null;
	}

}
