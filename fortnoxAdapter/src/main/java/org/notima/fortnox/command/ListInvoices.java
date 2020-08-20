package org.notima.fortnox.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.fortnox.command.table.InvoiceHeaderTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "list-fortnox-invoices", description = "Lists invoices in Fortnox")
@Service
@SuppressWarnings("rawtypes")
public class ListInvoices implements Action {

	@Reference 
	Session sess;
	
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Option(name = "-e", aliases = {
	"--enrich" }, description = "Read the complete invoice, not just the subset", required = false, multiValued = false)
	private boolean enrich;
	
	@Option(name = "--unbooked", description = "Show unbooked invoices", required = false, multiValued = false)
	private boolean unbooked;
	
	@Option(name = "--show-cancelled", description = "Show cancelled invoices", required = false, multiValued = false)
	private boolean showCancelled = false;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";
	
	@Override
	public Object execute() throws Exception {

		FactorySelector selector = new FactorySelector(bofs);
		
		BusinessObjectFactory bf = selector.getFactoryWithTenant(FortnoxAdapter.SYSTEMNAME, orgNo, null);

		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}
		
		@SuppressWarnings("unchecked")
		Map<Object, Object> invoicesMap = null;
		
		if (unbooked) {
			invoicesMap = bf.lookupList(FortnoxAdapter.LIST_UNPOSTED);
		} else {
			invoicesMap = bf.lookupList(FortnoxAdapter.LIST_UNPAID);
		}

		List<Object> invoices = new ArrayList<Object>();
		
		if (invoicesMap!=null) {
			
			Collection<Object> invoiceObjects = invoicesMap.values();
			
			Invoice inv = null;
			InvoiceSubset invs = null;
			for (Object oo : invoiceObjects) {
				if (oo instanceof Invoice) {
					inv = (Invoice)oo;
					if (!inv.isCancelled() || showCancelled) {
						invoices.add(oo);
					}
				}
				if (oo instanceof InvoiceSubset) {
					invs = (InvoiceSubset)oo;
					if (!invs.isCancelled() || showCancelled) {
						if (enrich) {
							inv = (Invoice)bf.lookupNativeInvoice(((InvoiceSubset)oo).getDocumentNumber());
							invoices.add(inv);
						} else {
							invoices.add(oo);
						}
					}
				}
				
			}
			
		}
		
		if (invoices.size()>0) {

			InvoiceHeaderTable iht = new InvoiceHeaderTable(invoices);
			iht.print(sess.getConsole());
			
			sess.getConsole().println("\n" + invoices.size() + " invoice(s).");
			
		}
		
		return null;
	}
	
	
	
}
