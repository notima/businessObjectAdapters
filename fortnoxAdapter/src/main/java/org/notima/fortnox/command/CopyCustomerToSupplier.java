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
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.Supplier;
import org.notima.businessobjects.adapter.fortnox.FortnoxConverter;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = _FortnoxCommandNames.SCOPE, name = _FortnoxCommandNames.CopyCustomerToSupplier, description = "Copy customer details to a vendor")
@Service
public class CopyCustomerToSupplier extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Option(name = "--overwrite-existing", description = "Overwrite existing (if any)", required = false, multiValued = false)
	private boolean overwriteExisting = false;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)
	private String orgNo = "";
	
	@Argument(index = 1, name = "customerNo", description ="The customer to copy", required = true, multiValued = false)
	private String customerNo = "";
	
	@Argument(index = 2, name = "supplierNo", description ="The supplier to write", required = true, multiValued = false)
	private String supplierNo = "";

	private FortnoxClient3	fortnoxApi;
	private Customer	sourceCustomer;
	private Supplier	targetSupplier;
	
	@Override
	public Object execute() throws Exception {

		fortnoxApi = getFortnoxClient(orgNo);
		if (fortnoxApi == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}

		lookupCustomer();
		
		String reply = sess.readLine("Do you want to copy customer  " + customerNo + " : " + sourceCustomer.getName() + " to a supplier? (y/n) ", null);

		if ("y".equalsIgnoreCase(reply)) {

			copyCustomer();
			writeVendor();
			sess.getConsole().println("Created supplier " + targetSupplier.getSupplierNumber() + " : " + targetSupplier.getName());
			
		}
		
		return null;
	}

	
	private void lookupCustomer() throws Exception {
		
		sourceCustomer = fortnoxApi.getCustomerByCustNo(customerNo);
		
	}

	private void writeVendor() throws Exception {
		
		targetSupplier = fortnoxApi.setSupplier(targetSupplier, !overwriteExisting);
		
	}

	private void copyCustomer() {
		
		targetSupplier = FortnoxConverter.convertCustomerToSupplier(sourceCustomer);
		if (supplierNo!=null) targetSupplier.setSupplierNumber(supplierNo);
		
	}
	
	
}
