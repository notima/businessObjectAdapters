package com.svea.businessobjects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.notima.generic.businessobjects.AccountingType;
import org.notima.generic.businessobjects.AccountingVoucherLine;

import com.svea.webpay.common.auth.SveaCredential;
import com.svea.webpay.common.reconciliation.AccountingReport;
import com.svea.webpay.common.reconciliation.AccountingVoucher;
import com.svea.webpay.common.reconciliation.FeeDetail;
import com.svea.webpay.common.reconciliation.PayoutLine;
import com.svea.webpay.common.reconciliation.RevenueLine;

public class AccountingReportConverter {

	/**
	 * Converts an accounting report to a list of accounting vouchers that can be used to map accounting to 
	 * an ERP-system.
	 * 
	 * @param ar		An accounting report.
	 * @return	A list of accounting vouchers.
	 */
	public static List<org.notima.generic.businessobjects.AccountingVoucher> toBoVouchers(AccountingReport ar) {
		
		List<org.notima.generic.businessobjects.AccountingVoucher> result = new ArrayList<org.notima.generic.businessobjects.AccountingVoucher>();
		if (ar==null || ar.getVouchers()==null) return result;
		
		org.notima.generic.businessobjects.AccountingVoucher dst;

		AccountingVoucherLine avl = null;
		List<AccountingVoucher> srcList = ar.getVouchers();
		for (AccountingVoucher src : srcList) {
			dst = new org.notima.generic.businessobjects.AccountingVoucher();
			dst.setDescription(src.getPaymentTypeReference());
			
			dst.setAcctDate(src.getAcctDate());
			result.add(dst);

			if (src.getRevenues()!=null) {
				for (RevenueLine rl : src.getRevenues()) {
					
					if (rl.getTaxAmount()!=0) {
						avl = new AccountingVoucherLine(BigDecimal.valueOf(-rl.getTaxAmount()), AccountingType.LIABILITY_VAT);
						avl.setTaxKey(rl.getTaxKey());
						dst.addVoucherLine(avl);
					}
					if (rl.getTaxBase()!=0) {
						avl = new AccountingVoucherLine(BigDecimal.valueOf(-rl.getTaxBase()), AccountingType.REVENUE);
						avl.setTaxKey(rl.getTaxKey());
						dst.addVoucherLine(avl);
					}
					
				}
			}
			
			if (src.getPayouts()!=null) {
				for (PayoutLine pl : src.getPayouts()) {
					
					if (pl.getFeeAmount()!=0) {
						avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getFeeAmount()), AccountingType.OTHER_EXPENSES_SALES);
						avl.setTaxKey(pl.getTaxKey());
						dst.addVoucherLine(avl);
					}
					if (pl.getTaxAmount()!=0) {
						avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getTaxAmount()), AccountingType.CLAIM_VAT);
						avl.setTaxKey(pl.getTaxKey());
						dst.addVoucherLine(avl);
					}
					if (pl.getOpeningBalance()<0) {
						if (pl.getPaidOut()>0) {
							 avl = new AccountingVoucherLine(BigDecimal.valueOf(-pl.getOpeningBalance()), AccountingType.LIABILITY_OTHER);
						} else {
							avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getPaidByCustomer()), AccountingType.LIABILITY_OTHER);
						}
						dst.addVoucherLine(avl);
					}
					if (pl.getPaidOut()>0) {
						avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getPaidOut()), AccountingType.LIQUID_ASSET_CASH);
						dst.addVoucherLine(avl);
					}
					
				}
			}
			
			// TODO: Have a better rounding algorithm.
			if (dst.getBalance().abs().doubleValue() > 4) {
				dst.balanceWithLine(AccountingType.UNKNOWN_BALANCE_TRX);
			} else {
				dst.balanceWithLine(AccountingType.ROUNDING);
			}
			
		}
		
		return result;
		
	}
	
}
