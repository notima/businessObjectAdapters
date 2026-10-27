package org.notima.businessobjects.adapter.tools.command;

import java.io.PrintStream;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.command.completer.PaymentFactoryCompleter;
import org.notima.businessobjects.adapter.tools.table.PaymentListTable;
import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;

/**
 * Lists the payments of the payment batches read from a file or a directory.
 * No matching is done since no destination is given.
 */
@Command(scope = "notima", name = "list-payments-in-batch", description = "Lists the payments in the payment batches of a file or directory, without matching")
@Service
public class ListPaymentsInBatch implements Action {

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	Session sess;

	@Argument(index = 0, name = "paymentFactory", description ="The payment source adapter name, e.g. P27", required = true, multiValued = false)
	@Completion(PaymentFactoryCompleter.class)
	private String paymentFactoryStr = "";

	@Argument(index = 1, name = "paymentSource", description ="A file, or a directory to read all applicable files in", required = true, multiValued = false)
	@Completion(FileCompleter.class)
	private String paymentSource = "";

	@Override
	public Object execute() throws Exception {

		PrintStream out = sess.getConsole();
		PaymentFactory paymentFactory = lookupPaymentFactory(paymentFactoryStr);
		if (paymentFactory==null) {
			out.println("Payment factory " + paymentFactoryStr + " not found.");
			return null;
		}

		List<PaymentBatch> batches = paymentFactory.readPaymentBatchesFromSource(paymentSource);
		if (batches==null || batches.isEmpty()) {
			out.println("No payment batches found in " + paymentSource);
			return null;
		}

		for (PaymentBatch pb : batches) {
			out.println();
			out.println(describe(pb));
			new PaymentListTable(pb).getShellTable().print(out);
		}

		return null;
	}

	/**
	 * Looks up the payment factory, ignoring case (p27 finds P27).
	 */
	private PaymentFactory lookupPaymentFactory(String systemName) {
		for (PaymentFactory pf : cof.listPaymentFactories()) {
			if (pf.getSystemName()!=null && pf.getSystemName().equalsIgnoreCase(systemName))
				return pf;
		}
		return null;
	}

	private String describe(PaymentBatch pb) {
		StringBuilder buf = new StringBuilder();
		buf.append(pb.getSource()!=null ? pb.getSource() : "Payment batch");
		if (pb.getPaymentType()!=null)
			buf.append(" | ").append(pb.getPaymentType());
		BankAccountDetail bad = pb.getBankAccount();
		if (bad!=null) {
			String acct = bad.getIBAN()!=null ? bad.getIBAN() : bad.getAccountNo();
			if (acct!=null)
				buf.append(" | Account ").append(acct);
			if (bad.getCurrency()!=null)
				buf.append(" ").append(bad.getCurrency());
		}
		if (pb.getPeriodFrom()!=null) {
			buf.append(" | ").append(pb.getPeriodFrom());
			if (pb.getPeriodTo()!=null && !pb.getPeriodTo().equals(pb.getPeriodFrom()))
				buf.append(" - ").append(pb.getPeriodTo());
		}
		if (pb.getBatchOwner()!=null && pb.getBatchOwner().getTaxId()!=null)
			buf.append(" | Owner ").append(pb.getBatchOwner().getTaxId());
		return buf.toString();
	}

}
