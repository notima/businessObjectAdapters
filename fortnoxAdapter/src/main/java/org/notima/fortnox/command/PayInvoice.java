package org.notima.fortnox.command;

import java.util.Calendar;
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
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.api.fortnox.entities3.ModeOfPayment;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "pay-fortnox-invoice", description = "Pays a specific invoice")
@Service
public class PayInvoice extends FortnoxCommand implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference 
	Session sess;

	@Option(name = "--paydate", description = "Use this date as paydate, default is today. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String payDateStr;

	@Option(name = "-iap", aliases = { "--invoicedate-as-paydate" }, description="Use invoice date as pay date", required = false, multiValued = false)
	private boolean iap = false;
	
	@Option(name = "--amount", description = "Pay using this amount. Default is open amount", required = false, multiValued = false)
	private Double amount;
	
	@Option(name = "--no-confirm", description = "Don't confirm anything. Default is to confirm", required = false, multiValued = false)
	private boolean noConfirm = false;
	
	@Option(name = "--no-bookkeep-payment", description = "Don't bookkeep payment.", required = false, multiValued = false)
	private boolean noBookkeepPayment = false;
	
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Argument(index = 1, name = "invoiceNo", description ="The invoice no", required = true, multiValued = false)
	private String invoiceNo;

	@Argument(index = 2, name = "modeOfPayment", description ="The mode of payment", required = true, multiValued = false)
	private String modeOfPayment;
	
	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
		
		Invoice invoice = fc.getInvoice(invoiceNo);

		if (invoice==null) {
			sess.getConsole().println("Invoice " + invoiceNo + " not found.");
			return null;
		}
		
		if (iap) {
			// Invoice date as pay date
			payDateStr = invoice.getInvoiceDate();
		}

		Date payDate = null;
		if (payDateStr==null) {
			payDate = Calendar.getInstance().getTime();
			payDateStr = FortnoxClient3.s_dfmt.format(payDate);
		} else {
			try {
				payDate = FortnoxClient3.s_dfmt.parse(payDateStr);
			} catch (Exception e) {
				sess.getConsole().println("Invalid date: " + payDateStr);
			}
		}
		
		if (!invoice.isBooked()) {
			String reply = noConfirm ? "y" : sess.readLine("The invoice is not booked. Do you want to book it? (y/n) ", null);
			if (reply.equalsIgnoreCase("y")) {
				
				try {
					fc.bookkeepInvoice(invoiceNo);
				} catch (Exception e) {
					String msg = null;
					if (e instanceof FortnoxException) {
						msg = ((FortnoxException)e).getMessage();
					} else {
						msg = e.getMessage();
					}
					sess.getConsole().println("Booking failed: " + msg);
					return null;
				}
				
			} else {
				sess.getConsole().println("Payment cancelled");
				return null;
			}
		}

		// Check open amount
		Double openAmt = invoice.getBalance();
		
		// Get mode of payment
		ModeOfPayment mp = null;
		try {
			mp = new ModeOfPayment(fc.getModeOfPayment(modeOfPayment));
		} catch (Exception e) {
			String msg = null;
			if (e instanceof FortnoxException) {
				msg = ((FortnoxException)e).getMessage();
			} else {
				msg = e.getMessage();
			}
			sess.getConsole().println("Invalid mode of payment ["+ modeOfPayment +"] : " + msg);
			return null;
		}

		// Confirm payment
		String reply = noConfirm ? "y" : sess.readLine("Do you want to pay invoice " + invoiceNo + " on " + payDateStr + " with amt " + openAmt + " using account " + mp.getAccountNumber() + ": (y/n) ", null);

		if (reply.equalsIgnoreCase("y")) {
			
			try {
				
			} catch (Exception e) {
				String msg = null;
				if (e instanceof FortnoxException) {
					msg = ((FortnoxException)e).getMessage();
				} else {
					msg = e.getMessage();
				}
				sess.getConsole().println("Payment failed: " + msg);
				return null;
			}
			
		} else {
			sess.getConsole().println("Payment cancelled");
			return null;
		}
		
		InvoicePayment pmt = null;
		
		try {
			pmt = fc.payCustomerInvoice(
					Integer.parseInt(invoiceNo), 
					mp, 
					payDate, 
					openAmt, 
					null, !noBookkeepPayment);
			
			if (pmt!=null) {
				sess.getConsole().println("Payment # " + pmt.getNumber() + " created.");
			} else {
				sess.getConsole().println("No payment was created.");
			}
			
		} catch (FortnoxException fe) {
			sess.getConsole().println(fe.toString());
		}
		
		
		return null;
	}
	
	
}
