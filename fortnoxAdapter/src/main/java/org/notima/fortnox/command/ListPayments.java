package org.notima.fortnox.command;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.api.fortnox.entities3.InvoicePaymentSubset;
import org.notima.api.fortnox.entities3.InvoicePayments;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.fortnox.command.table.InvoicePaymentHeaderTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "list-fortnox-payments", description = "Lists open payments in Fortnox")
@Service
@SuppressWarnings("rawtypes")
public class ListPayments extends FortnoxCommand implements Action {

	@Reference 
	Session sess;
	
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Option(name = "-e", aliases = {
	"--enrich" }, description = "Read the complete invoice, not just the subset", required = false, multiValued = false)
	private boolean enrich = false;
	
	@Option(name = "--booked", description = "Include booked payments", required = false, multiValued = false)
	private boolean booked = false;
	
	@Option(name = "--fromdate", description = "Select payments from this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String fromDateStr;
	
	@Option(name = "--untildate", description = "Select payments until this date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String untilDateStr;
	
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
		
		Date fromDate = null, untilDate = null;
		
		if (fromDateStr!=null) {
			fromDate = FortnoxClient3.s_dfmt.parse(fromDateStr);
		}
		if (untilDateStr!=null) {
			untilDate = FortnoxClient3.s_dfmt.parse(untilDateStr);
		}
			
		FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
		InvoicePayments payments = fc.getInvoicePayments();
		
		if (payments.getInvoicePaymentSubset()!=null) {

			InvoicePayment p;
			List<Object> ps = new ArrayList<Object>();
			for (InvoicePaymentSubset ips : payments.getInvoicePaymentSubset()) {
				if (!booked) {
					if (ips.getBooked()) {
						continue;
					}
				}
				if (fromDate!=null) {
					if (FortnoxClient3.s_dfmt.parse(ips.getPaymentDate()).before(fromDate)) {
						continue;
					}
				}
				
				if (untilDate!=null) {
					if (FortnoxClient3.s_dfmt.parse(ips.getPaymentDate()).after(untilDate)) {
						continue;
					}
				}
				
				if (enrich) {
					p = fc.getInvoicePayment(ips.getNumber());
					ps.add(p);
				} else {
					ps.add(ips);
				}
			}
			
			InvoicePaymentHeaderTable iht = new InvoicePaymentHeaderTable(ps);
			iht.print(sess.getConsole());
			
			sess.getConsole().println("\n" + ps.size() + " payment(s).");
			
		}
		
		return null;
	}
	
	
}
