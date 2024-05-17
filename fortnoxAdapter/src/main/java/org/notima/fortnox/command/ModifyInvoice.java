package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxConstants;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.api.fortnox.entities3.Invoices;
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

    @Argument(index = 3, name = "value", description = "The new value for the property", required = false, multiValued = false)
    private String newValue;

	private FortnoxClient3 fortnoxClient;
	
	private Invoice		   invoiceToModify;

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
		invoiceToModify = fortnoxClient.getInvoice(invoiceNo);

        if(invoiceToModify == null){
            sess.getConsole().println("Can't get invoice " + invoiceNo);
            return null;
        }

        String clientName = fortnoxClient.getCompanySetting().getName();
		
		String reply = noConfirm ? "y" : sess.readLine("Do you want to modify invoice " + invoiceNo + " " + invoiceToModify.getCustomerName() + " for client " + clientName + "? (y/n) ", null);
		if (!reply.equalsIgnoreCase("y")) {
			sess.getConsole().println("Modification cancelled.");
			return null;
		}

		modifyPropertySingle();

		return null;

    }

	private void modifyPropertySingle() throws Exception {

        switch (propertyToModify){
			
			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_WAREHOUSE_READY:
				try {
					invoiceToModify = fortnoxClient.warehouseReadyInvoice(invoiceNo);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_FIX_COMMENT_LINES:
				int count = invoiceToModify.fixInvoiceLines();
				if (count>0) {
					try {
						fortnoxClient.setInvoice(invoiceToModify);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				sess.getConsole().println(count + " lines adjusted.");
				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_DUE_DATE:
				String olddueDate = invoiceToModify.getDueDate();

				invoiceToModify.setDueDate(newValue);
				try {
					fortnoxClient.setInvoice(invoiceToModify);
				} catch (Exception e) {
					e.printStackTrace();
				}
				sess.getConsole().println("Old due date: " + olddueDate + ", New due date: " + invoiceToModify.getDueDate());

				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_INVOICE_DATE:
				String oldInvoiceDate = invoiceToModify.getInvoiceDate();

				invoiceToModify.setInvoiceDate(newValue);
				try {
					fortnoxClient.setInvoice(invoiceToModify);
				} catch (Exception e) {
					e.printStackTrace();
				}
				sess.getConsole().println(String.format("Old Invoice date: " + oldInvoiceDate + ", New invoice date: " + invoiceToModify.getInvoiceDate()));

				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_COPY_PAYMENTTERM_TO_INVOICE:
				changePaymentTermToCustomerPaymentTerm();
				break;
				
			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_PAYMENTTERM:
				changePaymentTerm();
				break;
				
			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_COPY_CUSTOMER_NAME_TO_INVOICE:
				copyCustomerNameToInvoice();
				break;
				
			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_EXTREF1:
				updateExtRef1();
				break;
				
			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_EXTREF2:
				updateExtRef2();
				break;
				
			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_COMMENT:
				updateComment();
				break;
				
			default:
				sess.getConsole().println(String.format("%s is not a modifiable property", propertyToModify));
            	break;
		}
	}

	private void changePaymentTermToCustomerPaymentTerm() throws Exception {
		
		Customer customer = fortnoxClient.getCustomerByCustNo(invoiceToModify.getCustomerNumber());
		newValue = customer.getTermsOfPayment();
		changePaymentTerm();
	}

	private void changePaymentTerm() throws Exception {
		invoiceToModify.setTermsOfPayment(newValue);
		fortnoxClient.setInvoice(invoiceToModify);
	}
	
	private void updateExtRef1() throws Exception {
		invoiceToModify.setExternalInvoiceReference1(newValue);
		fortnoxClient.setInvoice(invoiceToModify);
	}
	
	private void updateExtRef2() throws Exception {
		invoiceToModify.setExternalInvoiceReference2(newValue);
		fortnoxClient.setInvoice(invoiceToModify);
	}
	
	private void updateComment() throws Exception {
		invoiceToModify.setComments(newValue);
		fortnoxClient.setInvoice(invoiceToModify);
	}
	
	private void copyCustomerNameToInvoice() throws Exception {
		
		Customer customer = fortnoxClient.getCustomerByCustNo(invoiceToModify.getCustomerNumber());
		invoiceToModify.setCustomerName(customer.getName());
		fortnoxClient.setInvoice(invoiceToModify);
		
	}
	
	private Object modifyPropertyAll() throws Exception {
		
		Invoices invoices = null;

		try{
			invoices = fortnoxClient.getInvoices(FortnoxConstants.FILTER_UNBOOKED);
		}catch (Exception e1){
			e1.printStackTrace();
		}

		for(InvoiceSubset invoiceSubset : invoices.getInvoiceSubset()){
			try{
				invoiceToModify = (Invoice)bf.lookupNativeInvoice(((InvoiceSubset)invoiceSubset).getDocumentNumber());
			} catch(Exception e2){
				e2.printStackTrace();
			}

			modifyPropertySingle();

		}
		return null;

	}
}
