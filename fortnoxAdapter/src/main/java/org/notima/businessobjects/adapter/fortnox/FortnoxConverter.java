package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherRow;
import org.notima.api.fortnox.entities3.WriteOff;
import org.notima.api.fortnox.entities3.WriteOffs;
import org.notima.generic.businessobjects.BasicBusinessObjectConverter;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentWriteOff;

public class FortnoxConverter extends BasicBusinessObjectConverter {

	@Override
	public Invoice fromNativeInvoice(Object src) throws Exception {
		return FortnoxAdapter.convert((org.notima.api.fortnox.entities3.Invoice)src);
	}

	/**
	 * Creates a single transaction voucher with a vat amount. 
	 * 
	 * @param creditAcct
	 * @param debitAcct
	 * @param vatAcct
	 * @param totalAmount
	 * @param vatAmount
	 * @param description
	 * @return
	 */
	public Voucher createSingleCostWithVatTransactionVoucher(
			String voucherSeries,
			Date   acctDate,
			String creditAcct, 
			String debitAcct,
			String vatAcct,
			double totalAmount,
			double vatAmount,
			String description) {

		Voucher result = new Voucher();
		
		if (acctDate==null) {
			acctDate = Calendar.getInstance().getTime();
		}
		
		result.setDescription(description);
		result.setTransactionDate(FortnoxClient3.s_dfmt.format(acctDate));
		if (voucherSeries!=null) {
			result.setVoucherSeries(voucherSeries);
		}
		
		VoucherRow r = new VoucherRow();
		r.setAccount(Integer.parseInt(creditAcct));
		r.setCredit(totalAmount);
		result.addVoucherRow(r);
		r = new VoucherRow();
		r.setAccount(Integer.parseInt(debitAcct));
		r.setDebit(totalAmount - vatAmount);
		result.addVoucherRow(r);
		r = new VoucherRow();
		r.setAccount(Integer.parseInt(vatAcct));
		r.setDebit(vatAmount);
		result.addVoucherRow(r);
		
		return result;
		
	}
	
	/**
	 * Creates a single transaction voucher.
	 * 
	 * @param voucherSeries
	 * @param creditAcct
	 * @param debitAcct
	 * @param acctDate
	 * @param amount
	 * @param description
	 * @return
	 */
	public Voucher createSingleTransactionVoucher(
			String voucherSeries,
			Date   acctDate,
			String creditAcct, 
			String debitAcct, 
			double amount, 
			String description) {

		Voucher result = new Voucher();
		
		if (acctDate==null) {
			acctDate = Calendar.getInstance().getTime();
		}
		
		result.setDescription(description);
		result.setTransactionDate(FortnoxClient3.s_dfmt.format(acctDate));
		if (voucherSeries!=null)
			result.setVoucherSeries(voucherSeries);
		
		VoucherRow r = new VoucherRow();
		r.setAccount(Integer.parseInt(creditAcct));
		r.setCredit(amount);
		result.addVoucherRow(r);
		r = new VoucherRow();
		r.setAccount(Integer.parseInt(debitAcct));
		r.setDebit(amount);
		result.addVoucherRow(r);
		
		return result;
		
	}
	
	/**
	 * Converts a generic business object payment to a Fortnox Payment.
	 * 
	 * @param src	The Payment to be converted
	 * @return	A Fortnox equivalent of a payment.
	 * @throws	Exception if the payment is incomplete.
	 */
	public static InvoicePayment toFortnoxPayment(Payment src) throws Exception {
		
		if (src==null) return null;
		
		if (src.getInvoiceNo()==null)
			throw new Exception("Invoice number missing.");
		
		InvoicePayment dst = new InvoicePayment();
		
		dst.setAmount(src.getAmount());
		dst.setPaymentDate(FortnoxClient3.s_dfmt.format(src.getPaymentDate()));
		
		dst.setInvoiceNumber(Integer.parseInt(src.getInvoiceNo()));
		
		if (src.getCurrency()!=null && !"SEK".equalsIgnoreCase(src.getCurrency())) {
			
			dst.setAmountCurrency(src.getAmount());
			dst.setCurrency(src.getCurrency());
			
			if (src.getAcctAmount()!=null && src.getAcctAmount()!=0) {
				dst.setAmount(src.getAcctAmount());
				if (src.getAmount()!=0)
					dst.setCurrencyRate(src.getAcctAmount()/src.getAmount());
			}
			
		}
		
		// Check write offs
		if (src.getPaymentWriteOffs()!=null) {
			
			WriteOffs wofs = new WriteOffs();
			List<WriteOff> wlist = new ArrayList<WriteOff>();
			wofs.setWriteOff(wlist);
			dst.setWriteOffs(wofs);
			
			for (PaymentWriteOff po : src.getPaymentWriteOffs().getPaymentWriteOff()) {
				
				wlist.add(toFortnoxWriteOff(po));
				
			}
			
		}
		
		return dst;
	}
	
	
	public static WriteOff toFortnoxWriteOff(PaymentWriteOff src) {
		
		WriteOff dst = new WriteOff();
		
		dst.setAmount(src.getAmount());
		dst.setAccountNumber(src.getAccountNo());
		dst.setTransactionInformation(src.getComment());
		
		return dst;
		
	}
	
}
