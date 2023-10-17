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
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;
import org.notima.fortnox.command.table.InvoiceHeaderTable;

@Command(scope = _FortnoxCommandNames.SCOPE, name = _FortnoxCommandNames.ListInvoices, description = "Lists invoices in Fortnox")
@Service
public class ListInvoices extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Option(name = "-e", aliases = {
	"--enrich" }, description = "Read the complete invoice, not just the subset", required = false, multiValued = false)
	private boolean enrich;

	@Option(name = "--all", description = "Show all invoices", required = false, multiValued = false)
	private boolean all;
	
	@Option(name = _FortnoxOptions.PaymentTerm, description = "Filter on payment term", required = false, multiValued = false)
	private String paymentTerm;
	
	@Option(name = _FortnoxOptions.FromDate, description = "Select invoices from this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDateStr;
	
	@Option(name = _FortnoxOptions.UtilDate, description = "Select invoices until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String untilDateStr;
	
	@Option(name = "--unbooked", description = "Show unbooked invoices", required = false, multiValued = false)
	private boolean unbooked;
	
	@Option(name = "--show-cancelled", description = "Show cancelled invoices", required = false, multiValued = false)
	private boolean showCancelled = false;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)
	private String orgNo = "";

	private Map<Object, Object> invoicesMap = null;
	private List<InvoiceInterface> invoices; 
	
	
	@Override
	public Object execute() throws Exception {

		this.getBusinessObjectFactoryForOrgNo(orgNo);

		if (bf==null) {
			sess.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}

		parseDates(fromDateStr, untilDateStr);
		
		invoices = new ArrayList<InvoiceInterface>();
		
		if (!all) {
			if (unbooked) {
				invoicesMap = bf.lookupList(FortnoxAdapter.LIST_UNPOSTED);
			} else {
				invoicesMap = bf.lookupList(FortnoxAdapter.LIST_UNPAID);
			}
		} else {
			
			FortnoxClient3 fc = getFortnoxClient(orgNo);
			
			Invoices allInvoices = fc.getAllCustomerInvoicesByDateRange(fromDate, untilDate);
			if (allInvoices.getInvoiceSubset()!=null) {
				invoices.addAll(allInvoices.getInvoiceSubset());
			}
			
		}
		
		checkCancelledAndDateRange();
		enrichIfNecessary();
		checkPaymentTerm();
		
		if (invoices.size()>0) {

			InvoiceHeaderTable iht = new InvoiceHeaderTable(invoices);
			iht.getShellTable().print(sess.getConsole());
			
			sess.getConsole().println("\n" + invoices.size() + " invoice(s).");
			
		}
		
		return null;
	}
	
	private void checkCancelledAndDateRange() throws Exception {
		
		if (invoicesMap!=null) {
			
			Collection<InvoiceInterface> invoiceObjects = new ArrayList<InvoiceInterface>();
			
			for (Object o :	invoicesMap.values()) {
				if (o instanceof InvoiceInterface) {
					invoiceObjects.add((InvoiceInterface)o);
				}
			}
			
			Invoice inv = null;
			InvoiceSubset invs = null;
			for (InvoiceInterface oo : invoiceObjects) {
				
				// Check date filter
				if (!FortnoxUtil.isInDateRange(oo.getInvoiceDate(), fromDate, untilDate))
					continue;
				
				if (oo instanceof Invoice) {
					inv = (Invoice)oo;
					if (!inv.isCancelled() || showCancelled) {
						invoices.add(oo);
					}
				}
				if (oo instanceof InvoiceSubset) {
					invs = (InvoiceSubset)oo;
					if (!invs.isCancelled() || showCancelled) {
						invoices.add(oo);
					}
				}
				
			}
			
		}
	
	}

	
	private void enrichIfNecessary() throws Exception {
		if (!enrich && paymentTerm==null) return;
		List<InvoiceInterface> targetList = new ArrayList<InvoiceInterface>();
		Invoice inv;
		for (InvoiceInterface oo : invoices) {
			
			if (oo instanceof InvoiceSubset) {
				inv = (Invoice)bf.lookupNativeInvoice(((InvoiceSubset)oo).getDocumentNumber());
				targetList.add(inv);
			} else {
				targetList.add(oo);
			}
			
		}
		invoices = targetList;
	}

	/**
	 * Here we can assume all invoices are enriched.
	 * 
	 */
	private void checkPaymentTerm() {
		
		if (paymentTerm==null) return;
		
		List<InvoiceInterface> targetList = new ArrayList<InvoiceInterface>();
		Invoice inv;
		
		for (InvoiceInterface oo : invoices) {
			inv = (Invoice)oo;
			if (paymentTerm.equals(inv.getTermsOfPayment())) {
				targetList.add(inv);
			}
		}
		
		invoices = targetList;
	}
	
}
