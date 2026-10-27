package org.notima.businessobjects.adapter.tools.table;

import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;

/**
 * Lists the payments of a payment batch as they are read from the source, without matching.
 */
public class PaymentListTable extends GenericTable {

	public PaymentListTable(PaymentBatch batch) {

		addColumn("#", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Date");
		addColumn("Payer / Payee");
		addColumn("Account");
		addColumn("Reference");
		addColumn("Ref field");
		addColumn("Amount", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Curr");
		addColumn("Trx id");

		if (batch.isEmpty()) {
			setEmptyTableText("No payments");
			return;
		}

		int count = 0;
		double total = 0d;
		for (Payment<?> p : batch.getPayments()) {
			double amount = p.getAmount()!=null ? p.getAmount() : 0d;
			total += amount;
			GenericCell amountCell = new GenericCell(nfmt.format(amount));
			amountCell.setOriginalData(amount);
			addRow().addContent(
					++count,
					p.getPaymentDate()!=null ? dfmt.format(p.getPaymentDate()) : "-",
					p.getPayerName(),
					p.getAccountNo(),
					p.getDestinationSystemReference(),
					p.getDestinationSystemReferenceField(),
					amountCell,
					p.getCurrency(),
					p.getTransactionReference()!=null ? p.getTransactionReference().getTransactionId() : null);
		}

		addRow().addContent("=====", "==========", "==========", "==========", "==========", "==========", "==========", "====", "==========");
		GenericCell totalCell = new GenericCell(nfmt.format(total));
		totalCell.setOriginalData(total);
		addRow().addContent(count, "", "TOTAL", "", "", "", totalCell, "", "");

	}

}
