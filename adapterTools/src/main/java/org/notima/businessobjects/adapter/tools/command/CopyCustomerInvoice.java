package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.exception.AdapterNotFoundException;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "copy-customer-invoice", description = "Copy a customer invoices from one adapter to another")
@Service
public class CopyCustomerInvoice implements Action {

	@Reference
	private Session sess;
	
	@SuppressWarnings("rawtypes")
	@Reference
	protected List<BusinessObjectFactory> bofs;
	
    @Option(name = "--country-code", description="Country code if it needs to be specified", required = false, multiValued = false)
    protected String countryCode;
	
    @Argument(index = 0, name = "fromAdapter", description = "The from adapter to use", required = true, multiValued = false)
    @Completion(AdapterCompleter.class)
    protected String fromAdapter;

    @Argument(index = 1, name = "fromOrgNo", description = "The org no the from adapter", required = true, multiValued = false)
    protected String fromOrgNo;
    
    @Argument(index = 2, name = "toAdapter", description = "The target adapter to copy to", required = true, multiValued = false)
    @Completion(AdapterCompleter.class)
    protected String toAdapter;

    @Argument(index = 3, name = "toOrgNo", description = "The org no of the to adapter", required = true, multiValued = false)
    protected String toOrgNo;
    
    @Argument(index = 4, name = "invoiceNo", description = "The invoice(s) to copy from the from adapter", required = true, multiValued = true)
    protected String[] invoiceNo;
    
    @SuppressWarnings("rawtypes")
	private BusinessObjectFactory fromBof;
    @SuppressWarnings("rawtypes")    
    private BusinessObjectFactory toBof;
    
	@Override
	public Object execute() throws Exception {

		initFromAdapter();
		initToAdapter();
		copyInvoices();
		
		return null;
	}
	
	private void copyInvoices() throws Exception {
		
		if (invoiceNo!=null) {
			for (String no : invoiceNo) {
				copyInvoice(no);
			}
		}
		
	}
	
	private void copyInvoice(String invoiceNo) throws Exception {

		// If fromBof and toBof are the same adapter, we need to set tenant before fetching
		// the source invoice and before storing the target invoice
		
		fromBof.setTenant(fromOrgNo, countryCode);
		
		Invoice<?> invoice = fromBof.lookupInvoice(invoiceNo);
		if (invoice!=null) {
			if (invoice.getDocumentKey()!=null) {
				InvoiceLine il = new InvoiceLine();
				il.setDescription("Copy of " + invoice.getDocumentKey());
				invoice.addInvoiceLine(il);
			}
			// Unset invoice number since the new system will set that
			invoice.setId(null);
			invoice.setInvoiceKey(null);
			
			toBof.setTenant(toOrgNo, countryCode);
			toBof.persist(invoice);
			if (invoice.getId()!=null) {
				sess.getConsole().println("Copyed invoice to " + invoice.getId());
			}
		}
		
	}
	
	
	@SuppressWarnings("rawtypes")
	private BusinessObjectFactory getFactoryFor(String adapter) throws AdapterNotFoundException {
		BusinessObjectFactory bf = null;
		for (BusinessObjectFactory bff : bofs) {
			if (adapter.equalsIgnoreCase(bff.getSystemName())) {
				bf = bff;
				break;
			}
		}
		if (bf==null) throw(new AdapterNotFoundException(adapter));
		return bf;
	}
	
	private void initFromAdapter() throws AdapterNotFoundException {
		fromBof = getFactoryFor(fromAdapter);
	}
	
	private void initToAdapter() throws AdapterNotFoundException {
		toBof = getFactoryFor(toAdapter);
	}
	
	
}
