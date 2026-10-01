package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;
import org.notima.generic.ifacebusinessobjects.BusinessObjectConverter;
import org.apache.karaf.shell.api.action.Completion;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;

@Command(scope = "notima", name = "show-canonical-invoice", description = "Show a specific invoice")
@Service
public class ShowInvoice implements Action {

	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	private AdapterToolsSettings settings;
	
	@Reference 
	Session sess;

	@Argument(index = 0, name = "adapterName", description ="The adapter name", required = true, multiValued = false)
	private String adapterName = "";
	
	@Argument(index = 1, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo = "";

	@Argument(index = 2, name = "invoiceNo", description ="The invoice no", required = true, multiValued = false)
	private String invoiceNo;
	
    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;
    
    @Option(name = "-of", aliases = { "--outfile" }, description = "Write the invoice to file. 'default' writes ar-invoice-{invoiceNo}.xml (sales) or ap-invoice-{invoiceNo}.xml (vendor) to the tenant's default output directory", required = false, multiValued = false)
    private String outFile;
    
    @Option(name = "--destAdapter", description = "Destination adapter", required = false, multiValued = false)
    private String destAdapter;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object execute() throws Exception {
		
		Invoice<?> invoice = cof.lookupCustomerInvoice(adapterName, orgNo, countryCode, invoiceNo);
		
		if (invoice==null) {
			sess.getConsole().println("Invoice not found");
			return null;
		}
		
		Object invoiceNative = null;
		BusinessObjectConverter cv = null;
		
		if (destAdapter!=null) {
			invoiceNative = cof.convertToNativeInvoice(destAdapter, invoice, null);
			cv = cof.lookupConverter(destAdapter);
		}
		
		if ("default".equalsIgnoreCase(outFile)) {
			outFile = resolveDefaultOutFile(invoice);
			if (outFile==null) return null;
		}
		
		if (outFile!=null) {
			
			if (invoiceNative==null) {
				
				if (!outFile.endsWith(".xml")) {
					outFile += ".xml";
				}
				invoice.setNativeInvoice(null);
				JAXB.marshal(invoice, new File(outFile));
				sess.getConsole().println("Invoice written to " + outFile);
				
			} else {
				
				PrintWriter out = new PrintWriter(outFile);
				out.println(cv.nativeInvoiceToString(invoiceNative));
				out.close();
				sess.getConsole().println("Invoice written to " + outFile);
				
			}
		} else {
			
			if (invoiceNative==null) {
				StringWriter sw = new StringWriter();
				invoice.setNativeInvoice(null);
				JAXB.marshal(invoice,  sw);
				sess.getConsole().println(sw.toString());
			} else {
				sess.getConsole().println(cv.nativeInvoiceToString(invoiceNative));
			}
		}
		
		return null;
	}

	/**
	 * Returns the default file for the invoice in the tenant's default output directory.
	 * 
	 * @return	The path, or null (with a message printed) if no default output directory is set.
	 */
	private String resolveDefaultOutFile(Invoice<?> invoice) {
		
		String effectiveCountryCode = (countryCode != null && !countryCode.trim().isEmpty())
				? countryCode.trim()
				: settings.getDefaultCountryCode();
		TaxSubjectIdentifier tenantId = new TaxSubjectIdentifier(orgNo.trim(), effectiveCountryCode);
		
		TenantInformationFactory tif = cof.lookupTenantInformationFactory();
		TenantInformation ti = tif!=null ? tif.getTenantInformation(tenantId) : null;
		if (ti==null || ti.getDefaultOutputDirectory()==null || ti.getDefaultOutputDirectory().trim().isEmpty()) {
			sess.getConsole().println("No default output directory set for " + tenantId + ". Use set-tenant-info to set defaultOutputDirectory.");
			return null;
		}
		
		File dir = new File(ti.getDefaultOutputDirectory().trim());
		dir.mkdirs();
		String prefix = invoice.isSalesTransaction() ? "ar-invoice-" : "ap-invoice-";
		String invoiceKey = invoice.getInvoiceKey()!=null ? invoice.getInvoiceKey() : invoiceNo;
		return new File(dir, prefix + invoiceKey.replaceAll("[^a-zA-Z0-9_\\-]", "_") + ".xml").getPath();
	}
	
}
