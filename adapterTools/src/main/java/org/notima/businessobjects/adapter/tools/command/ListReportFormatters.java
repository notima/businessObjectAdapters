package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.ReportFormatter;

@Command(scope = "notima", name = "list-report-formatters", description = "Lists registered report formatters")
@Service
public class ListReportFormatters implements Action {

	@Reference
	private Session sess;
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<ReportFormatter> bofs;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {
	
		if (bofs==null) {
			System.out.println("No adapters registered");
		} else {
			for (ReportFormatter rf : bofs) {

				sess.getConsole().println(rf.getClazz().toString());
				
			}
			System.out.println(bofs.size() + " report formatters registered");
		}
		
		return null;
	}

}
