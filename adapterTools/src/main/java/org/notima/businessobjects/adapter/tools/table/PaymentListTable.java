package org.notima.businessobjects.adapter.tools.table;

import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;

/**
 * Lists the payments of payment batches as they are read from the source, without matching.
 */
public class PaymentListTable extends GenericTable {

	/**
	 * Creates a table of one batch with a total row, for printing.
	 */
	public PaymentListTable(PaymentBatch batch) {

		initColumns(false);

		if (batch.isEmpty()) {
			setEmptyTableText("No payments");
			return;
		}

		int count = 0;
		double total = 0d;
		for (Payment<?> p : batch.getPayments()) {
			total += amount(p);
			addPaymentRow(++count, null, p);
		}

		addRow().addContent("=====", "==========", "==========", "==========", "==========", "==========", "==========", "====", "==========");
		addRow().addContent(count, "", "TOTAL", "", "", "", amountCell(total), "", "");

	}

	/**
	 * Creates a table of all payments of the batches, with source and payment type
	 * columns and without total rows, for export to file.
	 */
	public PaymentListTable(List<PaymentBatch> batches) {

		initColumns(true);

		int count = 0;
		for (PaymentBatch batch : batches) {
			for (Payment<?> p : batch.getPayments()) {
				addPaymentRow(++count, batch, p);
			}
		}

		if (count==0) {
			setEmptyTableText("No payments");
		}

	}

	private void initColumns(boolean batchColumns) {
		addColumn("#", GenericColumn.ALIGNMENT_RIGHT);
		if (batchColumns) {
			addColumn("Source");
			addColumn("Type");
		}
		addColumn("Date");
		addColumn("Payer / Payee");
		addColumn("Account");
		addColumn("Reference");
		addColumn("Ref field");
		addColumn("Amount", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Curr");
		addColumn("Trx id");
	}

	/**
	 * @param batch		If not null, the batch's source and payment type are added.
	 */
	private void addPaymentRow(int count, PaymentBatch batch, Payment<?> p) {
		List<Object> content = new ArrayList<Object>();
		content.add(count);
		if (batch!=null) {
			content.add(batch.getSource());
			content.add(batch.getPaymentType());
		}
		content.add(p.getPaymentDate()!=null ? dfmt.format(p.getPaymentDate()) : "-");
		content.add(p.getPayerName());
		content.add(p.getAccountNo());
		content.add(p.getDestinationSystemReference());
		content.add(p.getDestinationSystemReferenceField());
		content.add(amountCell(amount(p)));
		content.add(p.getCurrency());
		content.add(p.getTransactionReference()!=null ? p.getTransactionReference().getTransactionId() : null);
		addRow().addContent(content);
	}

	private static double amount(Payment<?> p) {
		return p.getAmount()!=null ? p.getAmount() : 0d;
	}

	private static GenericCell amountCell(double amount) {
		GenericCell cell = new GenericCell(nfmt.format(amount));
		cell.setOriginalData(amount);
		return cell;
	}

}
