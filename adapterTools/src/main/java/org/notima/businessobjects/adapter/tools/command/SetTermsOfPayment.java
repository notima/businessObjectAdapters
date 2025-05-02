package org.notima.businessobjects.adapter.tools.command;


import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "set-terms-of-payment", description = "Sets terms of payment for invoices")
@Service
public class SetTermsOfPayment extends AdapterCommand {
	
    @Argument(index = 2, name = "top", description = "The term of payment to set", required = true, multiValued = false)
    protected String termsOfPayment;

    @Argument(index = 3, name = "invoiceNumbers", description = "Invoice numbers to update", required = false, multiValued = true)
    protected String[] invoiceNumbers;

    @SuppressWarnings("rawtypes")
	private BusinessObjectFactory bf_global;
    
	@Override
	public Object onExecute() throws Exception {

		populateAdapters();
		readParameters();
		runSetTermsOfPayment();
		
		return null;
	}

	@SuppressWarnings({ "rawtypes" })
	private void runSetTermsOfPayment() throws Exception {

		if (adaptersToList.size()>0) {
			
			for (BusinessObjectFactory bf : adaptersToList) {
				
				bf_global = bf;
				bf_global.setTenant(orgNo, countryCode);
				
				setTermsOfPaymentForInvoices();
				
			}
				
		}
		
	}

	private void readParameters() {
		
	}
	
	private void setTermsOfPaymentForInvoices() {
		
	}
	
	private void setTermsOfPayment(String invoiceNo) throws Exception {
		
		Invoice<?> invoice = lookupInvoice(invoiceNo);
		if (invoice!=null) {
			invoice.setPaymentTermKey(termsOfPayment);
			bf_global.persist(invoice);
		}
		
	}
	
	
	private Invoice<?> lookupInvoice(String invoiceNo) throws Exception {
		
		return bf_global.lookupInvoice(invoiceNo);
		
	}
	
}
