package org.notima.businessobjects.adapter.tools.command.completer;

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Service
public class AdapterCompleter implements Completer {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Override
	@SuppressWarnings("rawtypes") 
	public int complete(Session session, CommandLine commandLine, List<String> candidates) {

		StringsCompleter delegate = new StringsCompleter();
		if (bofs!=null) {
			for (BusinessObjectFactory bf : bofs) {
				delegate.getStrings().add(bf.getSystemName());
			}
		}
		return delegate.complete(session, commandLine, candidates);
		
	}

	
	
}
