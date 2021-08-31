package com.svea.businessobjects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.AccountingType;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.util.Translator;

import com.svea.webpay.common.reconciliation.AccountingReport;
import com.svea.webpay.common.reconciliation.AccountingVoucher;
import com.svea.webpay.common.reconciliation.FeeDetail;
import com.svea.webpay.common.reconciliation.PayoutLine;
import com.svea.webpay.common.reconciliation.RevenueLine;

/**
 * Class that takes a webpay accounting report and translates it into business objects vouchers (common format).  
 * 
 * @author Daniel Tamm
 *
 */
public class AccountingReportConverter {

	// Default accounting precision.
	public static final int DEFAULT_PRECISION = 2;

	private double maxRounding = 4.0;
	
	public AccountingVoucherLine mapWebpayFeeTypesToAccountingType(String feeType, AccountingVoucherLine avl) {
		
		switch(feeType) {
		
			case FeeDetail.FEETYPE_KICKBACK:
				avl.setAcctType(AccountingType.REVENUE);
				avl.setTaxKey("0");
				break;
			case FeeDetail.FEETYPE_DEVIATIONS:
				avl.setAcctType(AccountingType.UNKNOWN_BALANCE_TRX);
				break;
			case FeeDetail.ACCTTYPE_DEPOSIT:
				avl.setAcctType(AccountingType.ASSET_DEPOSIT);
				break;
			default:
				avl.setAcctType(AccountingType.OTHER_EXPENSES_SALES);
		
		}
		
		return avl;
		
	}

	/**
	 * Converts an accounting report to a list of accounting vouchers that can be used to map accounting to 
	 * an ERP-system.
	 * 
	 * @param ar		An accounting report.
	 * @param lang		Language to use for texts in voucher. Can be null.
	 * 
	 * @return	A list of accounting vouchers.
	 */
	public List<org.notima.generic.businessobjects.AccountingVoucher> toBoVouchers(AccountingReport ar, String lang) {
		
		List<org.notima.generic.businessobjects.AccountingVoucher> result = new ArrayList<org.notima.generic.businessobjects.AccountingVoucher>();
		if (ar==null || ar.getVouchers()==null) return result;
		
		org.notima.generic.businessobjects.AccountingVoucher dst;

		AccountingVoucherLine avl = null;
		List<AccountingVoucher> srcList = ar.getVouchers();
		for (AccountingVoucher src : srcList) {
			dst = new org.notima.generic.businessobjects.AccountingVoucher();
			dst.setPrecision(DEFAULT_PRECISION);
			dst.setDescription("Svea Webpay " +  
					src.getPaymentTypeReference() + " " + Translator.getTranslation(src.getPaymentType(), lang));
			
			dst.setCostCenter(src.getCostCenter());
			dst.setProjectCode(src.getProjectCode());
			
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
						avl = new AccountingVoucherLine(BigDecimal.valueOf(-rl.getTaxBase()), "?".equals(rl.getTaxKey()) ? AccountingType.REVENUE_UNCLEAR : AccountingType.REVENUE);
						avl.setTaxKey(rl.getTaxKey());
						dst.addVoucherLine(avl);
					}
					
				}
			}
			
			if (src.getPayouts()!=null) {
				for (PayoutLine pl : src.getPayouts()) {
					
					if (pl.getFeeAmount()!=0) {
						
						if (pl.getFeeSpecification()==null || pl.getFeeSpecification().isEmpty()) {
							avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getFeeAmount()), AccountingType.OTHER_EXPENSES_SALES);
							avl.setTaxKey(pl.getTaxKey());
							dst.addVoucherLine(avl);
						} else {
							// We have a fee specification
							double unspecifiedFee = pl.getFeeAmount() - pl.getSpecifiedFeeAmount();
							if (unspecifiedFee!=0) {
								avl = new AccountingVoucherLine(BigDecimal.valueOf(unspecifiedFee), AccountingType.OTHER_EXPENSES_SALES);
								avl.setTaxKey(pl.getTaxKey());
								dst.addVoucherLine(avl);
							}
							
							// Iterate through fee specifications
							for (FeeDetail fd : pl.getFeeSpecification()) {
								
								avl = new AccountingVoucherLine(BigDecimal.valueOf(fd.getFee()), null);
								if (fd.getDescription()!=null && fd.getDescription().trim().length()>0) {
									avl.setDescription(fd.getDescription());
								}
								avl.setTaxKey(pl.getTaxKey());
								mapWebpayFeeTypesToAccountingType(fd.getFeeType(), avl);
								dst.addVoucherLine(avl);
								
							}
							
						}
						
					}
					if (pl.getTaxAmount()!=0) {
						avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getTaxAmount()), AccountingType.CLAIM_VAT);
						avl.setTaxKey(pl.getTaxKey());
						dst.addVoucherLine(avl);
					}
					
					// Opening balance is negative
					if (pl.getOpeningBalance()<0 && pl.getEndingBalance()>=0) {
						if (pl.getPaidOut()>0) {
							 avl = new AccountingVoucherLine(BigDecimal.valueOf(-pl.getOpeningBalance()), AccountingType.LIABILITY_OTHER);
						} else {
							avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getPaidByCustomer()), AccountingType.LIABILITY_OTHER);
						}
						dst.addVoucherLine(avl);
					}
					
					// Ending balance is negative
					if (pl.getEndingBalance()<0) {
						avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getEndingBalance()-pl.getOpeningBalance()), AccountingType.LIABILITY_OTHER);
						dst.addVoucherLine(avl);
					}
					
					if (pl.getPaidOut()>0) {
						avl = new AccountingVoucherLine(BigDecimal.valueOf(pl.getPaidOut()), AccountingType.LIQUID_ASSET_CASH);
						dst.addVoucherLine(avl);
					}
					
				}
			}
			
			if (dst.getBalance().abs().doubleValue() > maxRounding) {
				dst.balanceWithLine(AccountingType.UNKNOWN_BALANCE_TRX);
			} else {
				// Purge voucher (round to precision, remove zero lines and round the rest)
				dst.purge();
			}
			
		}
		
		return result;
		
	}
	
	public double getMaxRounding() {
		return maxRounding;
	}

	public AccountingReportConverter setMaxRounding(double maxRounding) {
		this.maxRounding = maxRounding;
		return this;
	}
	
}
