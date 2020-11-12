package org.notima.businessobjects.adapter.excel.completer;

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.notima.businessobjects.adapter.excel.ExcelAdapterConstants;


@Service
public class OutputFormatCompleter implements Completer {
	
	@Override
	public int complete(Session session, CommandLine commandLine, List<String> candidates) {

		StringsCompleter delegate = new StringsCompleter();
		for (int i = 0; i<ExcelAdapterConstants.outputFormats.length; i++) {
			delegate.getStrings().add(ExcelAdapterConstants.outputFormats[i]);
		}
		return delegate.complete(session, commandLine, candidates);
		
	}
	
	
}
