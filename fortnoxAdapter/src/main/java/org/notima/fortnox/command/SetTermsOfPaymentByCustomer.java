package org.notima.fortnox.command;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.api.fortnox.entities3.TermsOfPayment;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "set-terms-of-payment-by-customer", description = "Configure terms of payment (adds / changes description)")
@Service
public class SetTermsOfPaymentByCustomer extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)
	private String orgNo = "";

	@Argument(index = 1, name = "term", description ="The term of payment code set open invoices to", required = true, multiValued = false)
	private String term = "";

	@Argument(index = 2, name = "File of customer numbers", description = "A file with customer numbers", required = true, multiValued = false)
	private String	fileName;
	
	private Set<String> customerIds = new TreeSet<String>();
	
	private FortnoxClient3 fc;
	
	
	@Override
	public Object execute() throws Exception {
		
		try {
		
			fc = getFortnoxClient(orgNo);
			if (fc == null) {
				sess.getConsole().println("Can't get client for " + orgNo);
				return null;
			}
			
			TermsOfPayment mp = fc.getTermsOfPaymentByCode(term);
			if (mp!=null) {
				mp = new TermsOfPayment(mp);
			}
			
			if (mp==null) {
				
				sess.getConsole().println("Term " + term + " doesn't exist.");
				sess.getConsole().println("Operation cancelled.");
				
			} else {
				
				readCustomerIdsFromFile();
				setToPaymentTerm();
				
			}
			
		} catch (FortnoxException fe) {
			sess.getConsole().println(fe.toString());
		}
		
		return null;
	}
	
	
	private void readCustomerIdsFromFile() throws IOException {
		
		// Read filename as a list of customer ids
		BufferedReader fr = new BufferedReader(new FileReader(new File(fileName)));
		String line = null;
		while((line = fr.readLine())!=null) {
			customerIds.add(line.trim());
		}
		fr.close();
		
	}
	
	private void setToPaymentTerm() throws Exception {
		
		// Get existing invoices
		Map<Object,org.notima.api.fortnox.entities3.InvoiceSubset> invoiceMap = ((BusinessObjectFactory)bf).lookupList(FortnoxAdapter.LIST_UNPOSTED);
		InvoiceSubset is;
		Invoice<org.notima.api.fortnox.entities3.Invoice> invoice;
		org.notima.api.fortnox.entities3.Invoice nativeInvoice;
		
		for (Object invoiceKey : invoiceMap.keySet()) {
	
			is = invoiceMap.get(invoiceKey);

			if (customerIds.contains(is.getCustomerNumber())) {
				invoice = bf.lookupInvoice(is.getDocumentNumber());
				if (!term.equals(invoice.getPaymentTermKey())) {
					nativeInvoice = invoice.getNativeInvoice();
					nativeInvoice.setTermsOfPayment(term);
					fc.setInvoice(nativeInvoice);
					sess.getConsole().println("Changed payment term on invoice " + is.getDocumentNumber() + " to AG");
				}
			}

		}
		
		
	}
	
	
}
