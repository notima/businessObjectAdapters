package org.notima.businessobjects.adapter.tools.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PayoutLine;

public class PaymentBatchTable extends GenericTable {
	
    private static NumberFormat nfmt = new DecimalFormat("#,##0.00");	
    
    private void initColumns(boolean detailed) {
    	addColumn("#", GenericColumn.ALIGNMENT_RIGHT);
		if(detailed) {
			addColumn("Date");
			addColumn("Invoice Id");
			addColumn("Order Id");
			addColumn("Checkout Id"); 
			addColumn("Payer Name");
			addColumn("Client Order");
			addColumn("Paid amt", GenericColumn.ALIGNMENT_RIGHT);
		}
		else{
			addColumn("Total", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("Fees", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("VAT", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("Paid out", GenericColumn.ALIGNMENT_RIGHT);
		}
    }
    
    /**
     * Creates a generic table from a payment report.
     * 
     * @param pr			The payment report
     * @param detailed		If report should be detailed.
     * @throws ParseException
     */
    public PaymentBatchTable(PaymentBatch pr, boolean detailed) throws ParseException {

    	initColumns(detailed);
		List<GenericRow> rows = detailed ? getDetailedTableRows(pr) : getTableRows(pr);
		addRows(rows);
		
    }

    private void addRows(List<GenericRow> rows) {

		if (rows.size()==0 || rows==null) {
			setEmptyTableText("No report");
			return;
		}
		
		for (GenericRow row : rows) {
			addRowOfObjects(row.getContent());
		}
    	
    }
    
    /**
     * Gets table rows for report.
     * 
     * @param report
     * @return	A list of table rows
     * @throws ParseException
     */
    public static List<GenericRow> getTableRows(PaymentBatch report) throws ParseException {
    	
    	List<GenericRow> rows = new ArrayList<GenericRow>();

		if (report==null || report.getPayments().size()==0) {
			return rows;
		}
    	
    	GenericRow row = null;

		double totalPaidAmount = 0;
		double totalFees = 0, totalVat = 0, totalRec = 0;
		int totalCount = 0;

		for(PayoutLine payout : report.retrievePayoutLines()){

			if (!payout.isIncludedInOtherPayout()) {
				totalPaidAmount += payout.getPaidByCustomer();
				totalRec += payout.getPaidOut();
			}
			totalFees += payout.getFeeAmount();
			totalVat += payout.getTaxAmount();
			totalCount += payout.getTrxCount();

			row = new GenericRow();

			row.addContent(
				payout.getTrxCount(),
				(payout.isIncludedInOtherPayout() ? "(":"") + nfmt.format(payout.getPaidByCustomer()) + (payout.isIncludedInOtherPayout() ? ")":""),
				nfmt.format(payout.getFeeAmount()),
				nfmt.format(payout.getTaxAmount()),
				nfmt.format(payout.getPaidOut()));
			
			rows.add(row);
		}

		row = new GenericRow();
		row.addContent("","TOTALS", "====", "==========", "=========", "========", "==========");
		rows.add(row);
		row = new GenericRow();
		row.addContent("",
				"", 
				totalCount, 
				nfmt.format(totalPaidAmount), 
				nfmt.format(totalFees), 
				nfmt.format(totalVat), 
				nfmt.format(totalRec));
		rows.add(row);
    	
    	return rows; 
    }

	public static List<GenericRow> getDetailedTableRows(PaymentBatch report) throws ParseException {
    	
    	List<GenericRow> rows = new ArrayList<GenericRow>();
    	
    	int count = 0;
    	double totalPaidAmount = 0d;
    	
    	GenericRow row = null;
			
		for (Payment<?> d : report.getPayments()) {

			totalPaidAmount += d.getAmount();

			row = new GenericRow();
			row.addContent(
				count++,
				d.getPaymentDate(),
				d.getInvoiceNo(),
				d.getOrderNo(),
				d.getComment(),
				d.getPayerName(),
				d.getClientOrderNo(),
				nfmt.format(d.getAmount()));

			rows.add(row);
		}
			
		row = new GenericRow();
		row.addContent("=====","==========", "==========", "==========", "==========", "==========", "==========", "==========");
		rows.add(row);

		row = new GenericRow();
		row.addContent("", "", "", "", "", "", "TOTAL", nfmt.format(totalPaidAmount));
		rows.add(row);

		row = new GenericRow();
		row.addContent("", "", "", "", "", "", "", "");
		rows.add(row);
    	
    	return rows; 
    	
    }
}
