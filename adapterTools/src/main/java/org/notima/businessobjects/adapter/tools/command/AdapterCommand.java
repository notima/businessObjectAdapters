package org.notima.businessobjects.adapter.tools.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.ReportFormatter;
import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

public abstract class AdapterCommand extends AbstractAction {

	@Reference
	protected FormatterFactory	formatterFactory;
	
	protected ReportFormatter<GenericTable> reportFormatter;
	
    protected GenericTable printableReport;
	
	@SuppressWarnings("rawtypes")
	@Reference
	protected List<BusinessObjectFactory> bofs;
	
    @Option(name = _NotimaCmdOptions.OUTPUT_FILE_NAME_SHORT, description="Output to file name", required = false, multiValued = false)
    private String	outFile;
    
    @Option(name = _NotimaCmdOptions.FORMAT_SHORT, description="The format of file to be output", required = false, multiValued = false)
    private String format;
    
    @Option(name = "--country-code", description="Country code if it needs to be specified", required = false, multiValued = false)
    protected String countryCode;
	
    @Argument(index = 0, name = "adapter", description = "The adapter to use", required = true, multiValued = false)
    protected String systemName;

    @Argument(index = 1, name = "orgNo", description = "The org no of the tenant", required = true, multiValued = false)
    protected String orgNo;

    @SuppressWarnings("rawtypes")
	protected List<BusinessObjectFactory> adaptersToList;
    
    @SuppressWarnings("rawtypes")
	protected void populateAdapters() {
    	
		adaptersToList = new ArrayList<BusinessObjectFactory>();
		if (bofs!=null) {
			for (BusinessObjectFactory bf : bofs) {
				
				if (systemName==null || systemName.equals(bf.getSystemName())) {
					adaptersToList.add(bf);
				}
				
			}
		}
    	
    }
    
	@SuppressWarnings("unchecked")
	protected void initAndRunReportFormatter() throws Exception {

		if (format!=null && printableReport!=null) {
			
			// Try to find a report formatter
			reportFormatter = (ReportFormatter<GenericTable>) formatterFactory.getReportFormatter(GenericTable.class, format);
			
			if (reportFormatter!=null) {
				Properties props = new Properties();
				props.setProperty(BasicReportFormatter.OUTPUT_FILENAME, outFile);
				
				String of = reportFormatter.formatReport((GenericTable)printableReport, format, props);
				sess.getConsole().println("Output file to: " + of);
			} else {
				sess.getConsole().println("Can't find formatter for " + format);
			}
			
		}
		
	}
    
    
}
