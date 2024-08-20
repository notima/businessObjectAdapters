package org.notima.businessobjects.adapter.tools.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.ReportFormatter;
import org.notima.businessobjects.adapter.tools.table.AccountingVoucherListTable;
import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.ifacebusinessobjects.AccountingVoucherConverter;

@Command(scope = "notima", name = "show-accounting-vouchers-from-source", description = "Shows accounting vouchers from given source")
@Service
public class ShowAccountingVouchersFromSource implements Action {
	
	public static DateFormat	s_dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	private FormatterFactory	formatterFactory;
	
	@Reference
	private Session sess;
	
    @Option(name = _NotimaCmdOptions.OUTPUT_FILE_NAME_SHORT, description="Output to file name", required = false, multiValued = false)
    private String	outFile;
    
    @Option(name = _NotimaCmdOptions.FORMAT_SHORT, description="The format of file to be output", required = false, multiValued = false)
    private String format;
	
    @Argument(index = 0, name = "adapter", description = "The adapter to use", required = true, multiValued = false)
    protected String systemName;
    
    @Argument(index = 1, name = "source", description = "The source (normally a file)", required = true, multiValued = false)
    private String source;
    
    private AccountingVoucherConverter<String> avc;
    
    private List<AccountingVoucher> vouchers;
    
    @SuppressWarnings("unchecked")
	private void getAccountingVoucherConverter() {
    	avc = (AccountingVoucherConverter<String>) cof.lookupAccountingVoucherConverter(systemName);
    }
    
	
	@Override
	public Object execute() throws Exception {

		getAccountingVoucherConverter();

		avc.readSource(source);

		vouchers = avc.getAccountingVouchers();
		
		printVouchers();
		
		return null;
	}
	
	
	private void printVouchers() throws Exception {
		
		AccountingVoucherListTable ast = new AccountingVoucherListTable(vouchers, true);
		ast.getShellTable().print(sess.getConsole());
		
		if (format!=null) {
			
			// Try to find a report formatter
			@SuppressWarnings("unchecked")
			ReportFormatter<GenericTable> rf = (ReportFormatter<GenericTable>) formatterFactory.getReportFormatter(GenericTable.class, format);
			
			if (rf!=null) {
				Properties props = new Properties();
				props.setProperty(BasicReportFormatter.OUTPUT_FILENAME, outFile);

				// Don't format the numbers when exporting.
				ast = new AccountingVoucherListTable(vouchers, false);
				
				String of = rf.formatReport((GenericTable)ast, format, props);
				sess.getConsole().println("Output file to: " + of);
			} else {
				sess.getConsole().println("Can't find formatter for " + format);
			}
			
		}
		
		
	}
	
}
