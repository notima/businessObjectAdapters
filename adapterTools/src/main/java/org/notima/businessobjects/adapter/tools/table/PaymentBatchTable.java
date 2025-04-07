package org.notima.businessobjects.adapter.tools.table;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PayoutLine;
import org.notima.generic.businessobjects.exception.CurrencyMismatchException;
import org.notima.generic.businessobjects.exception.DateMismatchException;

public class PaymentBatchTable extends GenericTable {
	
    private static NumberFormat nfmt = new DecimalFormat("#,##0.00");
    private static DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd");
    
    private void initColumns(boolean detailed) {
    	addColumn("#", GenericColumn.ALIGNMENT_RIGHT);
		if(detailed) {
			addColumn("Date");
			addColumn("Invoice Id");
			addColumn("Order Id");
			addColumn("Dest reference"); 
			addColumn("Payer Name");
			addColumn("Client Order");
			addColumn("Paid amt", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("Orig amt", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("Matched invoice");
			addColumn("Open amt", GenericColumn.ALIGNMENT_RIGHT);
		}
		else{
			addColumn("Total", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("Fees", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("VAT", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("Paid out", GenericColumn.ALIGNMENT_RIGHT);
			addColumn("Curr");
		}
    }
    
    /**
     * Creates a generic table from a payment report.
     * 
     * @param pr			The payment report
     * @param detailed		If report should be detailed.
     * @throws ParseException
     * @throws CurrencyMismatchException 
     * @throws DateMismatchException 
     */
    public PaymentBatchTable(PaymentBatch pr, boolean detailed) throws ParseException, DateMismatchException, CurrencyMismatchException {

    	initColumns(detailed);
		List<GenericRow> rows = detailed ? getDetailedTableRows(pr) : getTableRows(pr);
		addRows(rows);
		
    }

    private void addRows(List<GenericRow> rows) {

		if (rows==null || rows.size()==0) {
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
     * @throws CurrencyMismatchException 
     * @throws DateMismatchException 
     */
    public static List<GenericRow> getTableRows(PaymentBatch report) throws ParseException, DateMismatchException, CurrencyMismatchException {
    	
    	List<GenericRow> rows = new ArrayList<GenericRow>();

		if (report==null || report.getPayments().size()==0) {
			return rows;
		}
    	
    	GenericRow row = null;

		double totalPaidAmount = 0;
		double totalFees = 0, totalVat = 0, totalRec = 0;
		int totalCount = 0;

		List<PayoutLine> payoutLines = report.retrievePayoutLines();
		
		if (payoutLines!=null) {
			for(PayoutLine payout : payoutLines){
	
				if (!payout.isIncludedInOtherPayout()) {
					totalPaidAmount += payout.getPaidByCustomer();
					totalRec += payout.getPaidOut();
				}
				totalFees += payout.getTotalFeeAmount();
				totalVat += payout.getTotalFeeTaxAmount();
				totalCount += payout.getTrxCount();
	
				row = new GenericRow();
	
				row.addContent(
					payout.getTrxCount(),
					(payout.isIncludedInOtherPayout() ? "(":"") + nfmt.format(payout.getPaidByCustomer()) + (payout.isIncludedInOtherPayout() ? ")":""),
					nfmt.format(payout.getTotalFeeAmount()),
					nfmt.format(payout.getTotalFeeTaxAmount()),
					nfmt.format(payout.getPaidOut()),
					payout.getCurrency()
					);
				
				rows.add(row);
			}
		}

		row = new GenericRow();
		row.addContent("","TOTALS", "====", "==========", "=========", "========", "==========");
		rows.add(row);
		row = new GenericRow();
		row.addContent(
				totalCount, 
				nfmt.format(totalPaidAmount), 
				nfmt.format(totalFees), 
				nfmt.format(totalVat), 
				nfmt.format(totalRec), "", "");
		rows.add(row);
    	
    	return rows; 
    }

	public static List<GenericRow> getDetailedTableRows(PaymentBatch report) throws ParseException {
    	
    	List<GenericRow> rows = new ArrayList<GenericRow>();
    	
    	if (report.isEmpty()) return rows;
    	
    	int count = 0;
    	double totalPaidAmount = 0d;
    	double totalOriginalAmount = 0d;
    	double totalOpenAmount = 0d;
    	
    	GenericRow row = null;
    	
		for (Payment<?> d : report.getPayments()) {

			totalPaidAmount += d.getAmount();
			totalOriginalAmount += d.getOriginalAmount();
			totalOpenAmount += d.getMatchedInvoiceOpenAmount();

			row = new GenericRow();
			row.addContent(
				count++,
				dfmt.format(d.getPaymentDate()),
				d.getInvoiceNo(),
				d.getOrderNo(),
				d.getDestinationSystemReference(),
				d.getPayerName(),
				d.getClientOrderNo(),
				nfmt.format(d.getAmount()),
				nfmt.format(d.getOriginalAmount()),
				d.getMatchedInvoiceNo(),
				(d.hasMatchedInvoiceNo() ? nfmt.format(d.getMatchedInvoiceOpenAmount()) : "")
				);

			rows.add(row);
		}
			
		row = new GenericRow();
		row.addContent("=====","==========", "==========", "==========", "==========", "==========", "==========", "==========", "==========", "==========", "==========");
		rows.add(row);

		row = new GenericRow();
		row.addContent("", "", "", "", "", "", "TOTAL", nfmt.format(totalPaidAmount), nfmt.format(totalOriginalAmount), "", nfmt.format(totalOpenAmount));
		rows.add(row);

		row = new GenericRow();
		row.addContent("", "", "", "", "", "", "", "", "", "", "");
		rows.add(row);
    	
    	return rows; 
    	
    }
}
