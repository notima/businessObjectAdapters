package org.notima.businessobjects.adapter.tools.table;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.PaymentBatchProcessResult;
import org.notima.generic.businessobjects.PaymentProcessResult;

/**
 * Shows the invoice payments created in the destination system (or, in a dry run,
 * the payments that would have been created).
 */
public class PaymentProcessResultTable extends GenericTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");

	public PaymentProcessResultTable(List<PaymentProcessResult> results) {

		addColumn("#", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Reference");
		addColumn("Invoice");
		addColumn("Pay date");
		addColumn("Amount", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Curr");
		addColumn("Acct amt", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Mode of pmt");
		addColumn("Write-off", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Result");
		addColumn("Pmt no");
		addColumn("Note");

		if (results==null || results.isEmpty()) {
			setEmptyTableText("No payments");
			return;
		}

		int lineNo = 1;
		for (PaymentProcessResult r : results) {
			addRow().addContent(
					lineNo++,
					nvl(r.getSourceReference()),
					nvl(r.getInvoiceNo()),
					r.getPaymentDate()!=null ? r.getPaymentDate().toString() : "",
					nfmt.format(r.getAmount()),
					nvl(r.getCurrency()),
					r.getAcctAmount()!=null ? nfmt.format(r.getAcctAmount()) : "",
					nvl(r.getModeOfPayment()),
					r.getWriteOffAmount()!=0 ? nfmt.format(r.getWriteOffAmount()) : "",
					r.getResultCode()!=null ? r.getResultCode().name() : "",
					nvl(r.getDestinationPaymentId()),
					r.getTextResult()!=null ? r.getTextResult().toString() : ""
					);
		}

	}

	private static String nvl(String s) {
		return s!=null ? s : "";
	}

	/**
	 * Prints the payments and vouchers of the process results.
	 *
	 * @param results		The results of the processed batches.
	 * @param systemName	The name of the destination system.
	 * @param out			Where to print.
	 */
	public static void printResults(List<PaymentBatchProcessResult> results, String systemName, PrintStream out) {

		if (results==null || results.isEmpty()) return;

		List<PaymentProcessResult> payments = new ArrayList<PaymentProcessResult>();
		List<AccountingVoucher> vouchers = new ArrayList<AccountingVoucher>();
		boolean dryRun = false;
		boolean withoutErrors = true;
		int processed = 0;
		int matched = 0;
		for (PaymentBatchProcessResult r : results) {
			if (r==null) continue;
			payments.addAll(r.getPaymentResults());
			vouchers.addAll(r.getVouchers());
			dryRun = dryRun || r.isDryRun();
			withoutErrors = withoutErrors && r.isProcessedWithoutErrors();
			processed += r.getProcessedPaymentsCount();
			matched += r.getMatchedPaymentsCount();
		}

		String system = systemName!=null ? systemName : "destination";
		out.println();
		if (dryRun) {
			out.println("DRY RUN - nothing was written to " + system + ". This is what would have been created:");
		} else {
			out.println("Created in " + system + ":");
		}

		out.println();
		out.println("Invoice payments");
		new PaymentProcessResultTable(payments).getShellTable().print(out);

		out.println();
		out.println("Vouchers");
		if (vouchers.isEmpty()) {
			out.println("No vouchers");
		} else {
			new AccountingVoucherListTable(vouchers, true).getShellTable().print(out);
		}

		out.println();
		out.println(processed + " payments processed, " + matched + " matched"
				+ (withoutErrors ? "." : ". There were errors, see the Result column."));

	}

}
