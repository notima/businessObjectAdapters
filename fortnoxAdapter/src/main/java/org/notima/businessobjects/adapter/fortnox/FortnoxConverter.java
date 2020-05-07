package org.notima.businessobjects.adapter.fortnox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherRow;
import org.notima.api.fortnox.entities3.WriteOff;
import org.notima.api.fortnox.entities3.WriteOffs;
import org.notima.generic.businessobjects.AccountingType;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.BasicBusinessObjectConverter;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentWriteOff;
import org.notima.generic.businessobjects.util.LocalDateUtils;

/**
 * Converts Fortnox entities to/from Business Objects entities.
 * 
 * @author Daniel Tamm
 *
 */
public class FortnoxConverter extends BasicBusinessObjectConverter<Object, org.notima.api.fortnox.entities3.Invoice> {
	
	/**
	 * Converts a Fortnox Invoice to a Business Objects Invoice
	 * 
	 */
	@Override
	public Invoice<org.notima.api.fortnox.entities3.Invoice> fromNativeInvoice(org.notima.api.fortnox.entities3.Invoice src) throws Exception {
		return FortnoxAdapter.convert((org.notima.api.fortnox.entities3.Invoice)src);
	}

	/**
	 * Takes an accounting voucher and converts it into a Fortnox Voucher.
	 * If account numbers are not set in the source, default accounts from Fortnox are
	 * tried using the accountType and taxKey of the source lines.
	 * 
	 * @param   fa				FortnoxAdapter
	 * @param	voucherSeries	Voucher Series
	 * @param	src				The source to be converted
	 * 
	 */
	public Voucher mapFromBusinessObjectVoucher(
			FortnoxAdapter fa, 
			String voucherSeries,
			AccountingVoucher src) throws Exception {
		
		Voucher dst = new Voucher();
		if (src.getAcctDate()==null) {
			src.setAcctDate(LocalDate.now());
		}
		
		dst.setDescription(src.getDescription());
		dst.setTransactionDate(FortnoxClient3.s_dfmt.format(LocalDateUtils.asDate(src.getAcctDate())));
		
		if (voucherSeries!=null) {
			dst.setVoucherSeries(voucherSeries);
		}
		
		if (src.getLines()==null || src.getLines().size()==0) {
			throw new Exception("Can't convert a voucher without lines.");
		}

		VoucherRow r = null;
		String taxKey = null;
		
		for (AccountingVoucherLine avl : src.getLines()) {
			
			r = new VoucherRow();
			// Try to map accountNo on account type
			if ((avl.getAcctNo()==null || avl.getAcctNo().trim().length()==0) 
					&& avl.getAcctType()!=null && avl.getAcctType().trim().length()>0) {
				
				taxKey = convertTaxKey(avl.getTaxKey());
				
				switch (avl.getAcctType()) {
					case AccountingType.REVENUE:
						avl.setAcctNo(fa.getRevenueAcctNo(taxKey, null));
						break;
					case AccountingType.LIABILITY_VAT:
						avl.setAcctNo(fa.getOutVatAccount(taxKey));
						break;
					case AccountingType.CLAIM_VAT:
						avl.setAcctNo(fa.getPredefinedAccount(FortnoxClient3.ACCT_INVAT));
						break;
					case AccountingType.ROUNDING:
						avl.setAcctNo(fa.getPredefinedAccount(FortnoxClient3.ACCT_ROUNDING));
						break;
					case AccountingType.OTHER_EXPENSES_SALES:
						// TODO: Below must be configurable
						avl.setAcctNo("6590");
						break;
					case AccountingType.LIQUID_ASSET_CASH:
						avl.setAcctNo(fa.getPredefinedAccount(FortnoxClient3.ACCT_CASHBYCARD));
				}
				
			} else {
				throw new Exception("Unable to map empty account / accountType");
			}
			
			if (avl.getAcctNo()!=null && avl.getAcctNo().trim().length()>0) {
				r.setAccount(Integer.parseInt(avl.getAcctNo()));
			}			

			r.setCredit(avl.getCreditAmount().doubleValue());
			r.setDebit(avl.getDebitAmount().doubleValue());
			if (avl.getDescription()!=null && avl.getDescription().trim().length()>0) {
				r.setDescription(avl.getDescription());
			}
			dst.addVoucherRow(r);
			
		}
		
		return dst;
		
	}
	
	/**
	 * Converts tax key from numeric vat rate to Fortnox tax key
	 * 
	 * @param taxKey
	 * @return	A converted tax key.
	 */
	public String convertTaxKey(String taxKey) {
		
		if (taxKey==null) return null;
		
		double vatRate = 0;
		try {
			vatRate = Double.parseDouble(taxKey);
		} catch (NumberFormatException pe) {
			return taxKey;
		}
		
		if (vatRate>16) {
			return "MP1";
		}
		if (vatRate>10) {
			return "MP2";
		}
		if (vatRate>5) {
			return "MP3";
		}
		
		if (vatRate==0) {
			return "MP4";
		}
		
		return "MP4";
		
	}
	
	
	/**
	 * Creates a single transaction voucher with a vat amount. 
	 * 
	 * @param voucherSeries		The voucher series to use
	 * @param acctDate			The accounting date
	 * @param creditAcct		The account to be credited.
	 * @param debitAcct			The account to be debited.
	 * @param vatAcct			Vat amount (if any)
	 * @param totalAmount		The total amount of the transaction
	 * @param vatAmount			The VAT amount.
	 * @param description		The description for the voucher text.
	 * @return	A Fortnox Voucher
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
		if (totalAmount>0) {
			r.setCredit(totalAmount);
		} else {
			r.setDebit(-totalAmount);
		}
		result.addVoucherRow(r);
		r = new VoucherRow();
		r.setAccount(Integer.parseInt(debitAcct));
		if (totalAmount>0) {
			r.setDebit(totalAmount - vatAmount);
		} else {
			r.setCredit(-totalAmount + vatAmount);
		}
		result.addVoucherRow(r);
		
		if (vatAmount!=0) {
			r = new VoucherRow();
			r.setAccount(Integer.parseInt(vatAcct));
			if (vatAmount>0)
				r.setDebit(vatAmount);
			else 
				r.setCredit(-vatAmount);
			
			result.addVoucherRow(r);
		}
		
		return result;
		
	}
	
	/**
	 * Creates a single transaction voucher with two lines (debet / credit)
	 * 
	 * @param voucherSeries		The voucher series to use
	 * @param acctDate			The accounting date
	 * @param creditAcct		The account to be credited.
	 * @param debitAcct			The account to be debited.
	 * @param amount		The total amount of the transaction
	 * @param description		The description for the voucher text.
	 * 
	 * @return	A Fortnox Voucher.
	 */
	public Voucher createSingleTransactionVoucher(
			String voucherSeries,
			Date   acctDate,
			String creditAcct, 
			String debitAcct, 
			double amount, 
			String description) {

		return FortnoxUtil.createSingleTransactionVoucher(voucherSeries, acctDate, creditAcct, debitAcct, amount, description);
		
	}
	
	/**
	 * Converts a generic business object payment to a Fortnox Payment.
	 * 
	 * @param src	The Payment to be converted
	 * @return	A Fortnox equivalent of a payment.
	 * @throws	Exception if the payment is incomplete.
	 */
	public static InvoicePayment toFortnoxPayment(Payment<?> src) throws Exception {
		
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
	
	/**
	 * Creates a Fortnox Write off
	 * 
	 * @param src		A Business Objects writeoff.
	 * @return	A Fortnox writeoff.
	 */
	public static WriteOff toFortnoxWriteOff(PaymentWriteOff src) {
		
		WriteOff dst = new WriteOff();
		
		dst.setAmount(src.getAmount());
		dst.setAccountNumber(src.getAccountNo());
		dst.setTransactionInformation(src.getComment());
		
		return dst;
		
	}
	
}
