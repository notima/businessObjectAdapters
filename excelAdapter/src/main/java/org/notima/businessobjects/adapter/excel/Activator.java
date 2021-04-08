package org.notima.businessobjects.adapter.excel;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.businessobjects.adapter.tools.ReportFormatter;
import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(ReportFormatter.class)
		}
)
public class Activator extends BaseActivator {

	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	public void doStart() throws Exception {
		super.doStart();
		
		ReportFormatter<GenericTable> reportFormatter = new ExcelReportFormatter();
		log.info("Created Excel Report Formatter for Generic Table");
		register(ReportFormatter.class, reportFormatter, null);
		log.info("Context: " + this.bundleContext.toString());
		
		
	}
	
	
}
