package org.notima.businessobjects.adapter.jasperreports.cmd;

import java.io.File;
import java.util.Properties;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.OrderListFormatter;
import org.notima.generic.businessobjects.OrderList;

@Command(scope = "jasperreport", name = "create-report", description = "Creates a report")
@Service
public class CreateReport implements Action {

	@Reference
	private FormatterFactory ff;

	@Reference
	private Session		sess;
	
	@Argument(index = 0, name = "source", description ="The source data (xml)", required = true, multiValued = false)
	@Completion(FileCompleter.class)
	String source = "";

	@Argument(index = 1, name = "report", description ="The report to be used (a .jasper file)", required = true, multiValued = false)
	@Completion(FileCompleter.class)
	String report = "";
	
	@Override
	public Object execute() throws Exception {

		OrderListFormatter olf = ff.getFormatter("pdf");
		if (olf==null) {
			sess.getConsole().println("No formatter available");
			return null;
		}
		
		Properties props = new Properties();
		props.put("JasperFile", report);
		
		OrderList orderList = JAXB.unmarshal(new File(source), OrderList.class);
		olf.formatOrderList(orderList, "pdf", props);
		
		return null;
	}

}
