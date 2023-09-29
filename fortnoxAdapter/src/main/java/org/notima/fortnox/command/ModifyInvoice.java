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
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.fortnox.command.completer.FortnoxInvoicePropertyCompleter;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "modify-fortnox-invoice", description = "Modify a specific invoice")
@Service
public class ModifyInvoice extends FortnoxCommand implements Action  {
	
	@Reference 
	Session session;
	
	@Option(name = "--no-confirm", description = "Don't confirm anything. Default is to confirm", required = false, multiValued = false)
	private boolean noConfirm = false;
	
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
	private Invoice invoice;

    @Override
    public Object execute() throws Exception {
        fortnoxClient = this.getFortnoxClient(orgNo);
		if (fortnoxClient == null) {
			session.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
		
		invoice = fortnoxClient.getInvoice(invoiceNo);

        if(invoice == null){
            session.getConsole().println("Can't get invoice " + invoiceNo);
            return null;
        }

        String clientName = fortnoxClient.getCompanySetting().getName();
		
		String reply = noConfirm ? "y" : session.readLine("Do you want to modify invoice " + invoiceNo + " " + invoice.getCustomerName() + " for client " + clientName + "? (y/n) ", null);
		if (!reply.equalsIgnoreCase("y")) {
			session.getConsole().println("Modification cancelled.");
			return null;
		}

		modifyProperty();

		return null;

    }

	private void modifyProperty() {

        switch (propertyToModify){
			
			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_WAREHOUSE_READY:
				try {
					invoice = fortnoxClient.warehouseReadyInvoice(invoiceNo);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_FIX_COMMENT_LINES:
				int count = invoice.fixInvoiceLines();
				if (count>0) {
					try {
						fortnoxClient.setInvoice(invoice);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				session.getConsole().println(count + " lines adjusted.");
				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_DUE_DATE:
				String olddueDate = invoice.getDueDate();

				invoice.setDueDate(newValue);
				try {
					fortnoxClient.setInvoice(invoice);
				} catch (Exception e) {
					e.printStackTrace();
				}
				session.getConsole().println("Old due date: " + olddueDate + ", New due date: " + invoice.getDueDate());

				break;

			case FortnoxInvoicePropertyCompleter.INVOICE_PROPERTY_INVOICE_DATE:
				String oldInvoiceDate = invoice.getInvoiceDate();

				invoice.setInvoiceDate(newValue);
				try {
					fortnoxClient.setInvoice(invoice);
				} catch (Exception e) {
					e.printStackTrace();
				}
				session.getConsole().println(String.format("Old Invoice date: " + oldInvoiceDate + ", New invoice date: " + invoice.getInvoiceDate()));

				break;

			default:
				session.getConsole().println(String.format("%s is not a modifiable property", propertyToModify));
            	break;
		}
	}
}
