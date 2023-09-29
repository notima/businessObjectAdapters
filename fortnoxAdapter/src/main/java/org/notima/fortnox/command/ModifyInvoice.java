package org.notima.fortnox.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoiceInterface;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.api.fortnox.entities3.Invoices;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.fortnox.command.completer.FortnoxInvoicePropertyCompleter;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "modify-fortnox-invoice", description = "Modify a specific invoice")
@Service
public class ModifyInvoice extends FortnoxCommand implements Action  {
	
	@Reference 
	Session sess;
	
	@Option(name = "--no-confirm", description = "Don't confirm anything. Default is to confirm", required = false, multiValued = false)
	private boolean noConfirm = false;

	@Option(name = "--all", description = "Modify all invoices according to filter (unbooked, TODO-date)", required = false, multiValued = false)
	private String filter;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	@Argument(index = 1, name = "invoiceNo", description ="The invoice no", required = true, multiValued = false)
	private String invoiceNo;

    @Argument(index = 2, name = "property", description = "The property to modify", required = true, multiValued = false)
    @Completion(FortnoxInvoicePropertyCompleter.class)
    private String propertyToModify;

    @Argument(index = 3, name = "value", description = "The new value for the property", required = true, multiValued = false)
    private String newValue;

	private FortnoxClient3 fortnoxClient;

    @Override
    public Object execute() throws Exception {
		fortnoxClient = this.getFortnoxClient(orgNo);
		if (fortnoxClient == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
		
		if (!(filter == null)){
			String reply = noConfirm ? "y" : sess.readLine("Do you want to modify invoice all unbooked invoices? (y/n) ", null);
			if (!reply.equalsIgnoreCase("y")) {
			sess.getConsole().println("Modification cancelled.");
			return null;
		}
			modifyPropertyAll();
			return null;
		}
		Invoice invoice = fortnoxClient.getInvoice(invoiceNo);

        if(invoice == null){
            sess.getConsole().println("Can't get invoice " + invoiceNo);
            return null;
        }

        String clientName = fortnoxClient.getCompanySetting().getName();
		
		String reply = noConfirm ? "y" : sess.readLine("Do you want to modify invoice " + invoiceNo + " " + invoice.getCustomerName() + " for client " + clientName + "? (y/n) ", null);
		if (!reply.equalsIgnoreCase("y")) {
			sess.getConsole().println("Modification cancelled.");
			return null;
		}


		modifyPropertySingle(invoice);



		return null;

    }

	private void modifyPropertySingle(Invoice invoiceModifier) {

        switch (propertyToModify){
			
			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_WAREHOUSE_READY:
				try {
					invoiceModifier = fortnoxClient.warehouseReadyInvoice(invoiceNo);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_FIX_COMMENT_LINES:
				int count = invoiceModifier.fixInvoiceLines();
				if (count>0) {
					try {
						fortnoxClient.setInvoice(invoiceModifier);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				sess.getConsole().println(count + " lines adjusted.");
				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_DUE_DATE:
				String olddueDate = invoiceModifier.getDueDate();

				invoiceModifier.setDueDate(newValue);
				try {
					fortnoxClient.setInvoice(invoiceModifier);
				} catch (Exception e) {
					e.printStackTrace();
				}
				sess.getConsole().println("Old due date: " + olddueDate + ", New due date: " + invoiceModifier.getDueDate());

				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_INVOICE_DATE:
				String oldInvoiceDate = invoiceModifier.getInvoiceDate();

				invoiceModifier.setInvoiceDate(newValue);
				try {
					fortnoxClient.setInvoice(invoiceModifier);
				} catch (Exception e) {
					e.printStackTrace();
				}
				sess.getConsole().println(String.format("Old Invoice date: " + oldInvoiceDate + ", New invoice date: " + invoiceModifier.getInvoiceDate()));

				break;

			default:
				sess.getConsole().println(String.format("%s is not a modifiable property", propertyToModify));
            	break;
		}
	}

	private Object modifyPropertyAll(){
		
		Invoices invoices = null;

		try{
			invoices = fortnoxClient.getInvoices(FortnoxClient3.FILTER_UNBOOKED);
		}catch (Exception e1){
			e1.printStackTrace();
		}

		for(InvoiceSubset invoiceSubset : invoices.getInvoiceSubset()){
			Invoice inv = null;
			try{
				inv = (Invoice)bf.lookupNativeInvoice(((InvoiceSubset)invoiceSubset).getDocumentNumber());
			} catch(Exception e2){
				e2.printStackTrace();
			}

			modifyPropertySingle(inv);

		}
		return null;

	}
}
