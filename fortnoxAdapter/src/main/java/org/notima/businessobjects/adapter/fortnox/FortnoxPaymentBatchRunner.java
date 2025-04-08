package org.notima.businessobjects.adapter.fortnox;

import java.util.List;
import java.util.Map;

import org.jline.utils.Log;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxConstants;
import org.notima.api.fortnox.entities3.AccountSubset;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.api.fortnox.entities3.ModeOfPaymentSubset;
import org.notima.api.fortnox.entities3.ModesOfPayments;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.businessobjects.adapter.fortnox.FortnoxExtendedClient.ReferenceField;
import org.notima.businessobjects.adapter.fortnox.exception.UnableToDetermineFeeAccountException;
import org.notima.businessobjects.adapter.fortnox.exception.UnableToDetermineModeOfPaymentException;
import org.notima.generic.businessobjects.AccountingType;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchProcessOptions;
import org.notima.generic.businessobjects.PaymentBatchProcessResult;
import org.notima.generic.businessobjects.PaymentProcessResult;
import org.notima.generic.businessobjects.PaymentProcessResult.ResultCode;
import org.notima.generic.businessobjects.PaymentWriteOff;
import org.notima.generic.businessobjects.PayoutLine;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;

public class FortnoxPaymentBatchRunner {

	private FortnoxExtendedClient	extendedClient;	
	private TaxSubjectIdentifier	taxSubject;
	private PaymentBatch			paymentBatch;
	private String					modeOfPayment;
	private String					modeOfPaymentAccount;
	private String					modeOfPrepayment;
	private String					modeOfPrepaymentAccount;
	private String					feeGlAccount;
	private String					intransitAccount;
	private String					voucherSeries;
	
	private PaymentBatchProcessResult	paymentBatchProcessResult;
	private FortnoxConverter		fortnoxConverter;
	
	private boolean					dryRun = false;
	
	
	private Map<String, AccountSubset> acctMap;
	
	private PaymentBatchProcessOptions	processOptions = new PaymentBatchProcessOptions();
	
	public FortnoxPaymentBatchRunner(FortnoxExtendedClient client) throws Exception {
		extendedClient = client;
		CompanySetting cs = extendedClient.getCompanySetting();
		taxSubject = new TaxSubjectIdentifier(cs.getOrganizationNumber(), cs.getCountryCode());
		paymentBatchProcessResult = new PaymentBatchProcessResult();
		fortnoxConverter = new FortnoxConverter();
	}
	
	public void setPaymentBatch(PaymentBatch paymentBatch) throws Exception {
		this.paymentBatch = paymentBatch;
		// TODO - Compare tax subject
		determineModeOfPayment();

		determineAccountingSettings();
		
		// Check if account map needs to be updated
		if (acctMap==null) {
			refreshAccountMap();
		}
		
		determineFeeAccount();
		
	}
	
	public TaxSubjectIdentifier getTaxSubject() {
		return taxSubject;
	}

	public void processFees() throws Exception {
		
		if (processOptions.isAccountPayoutOnly() || processOptions.isDryRun() || processOptions.isFeesPerPayment()) {
			return;
		}
		
		if (processOptions.isDraftPaymentsIfPossible()) {
			return;
		}
		
	}
	
	public void processPayout() throws Exception {
		
		if (processOptions.isAccountFeesOnly() || processOptions.isDryRun() || processOptions.isDraftPaymentsIfPossible()) {
			return;
		}

		List<PayoutLine> payoutLines = paymentBatch.retrievePayoutLines();
		
		for (PayoutLine payout : payoutLines) {
			
			processPayoutLine(payout);
			
		}
		
		
	}

	private void processPayoutLine(PayoutLine payout) throws Exception {
		
		if (processOptions.isFeesPerPayment()) {
			// Clear fees here
			payout.setTotalFeeAmount(0.0);
			payout.setTotalFeeTaxAmount(0.0);
		}
		
		// If there are non specific fees (not tied to any specific payments), add these not
		payout.addPayoutFeesToFeeTotal();
		
		AccountingVoucher av = AccountingVoucher.buildVoucherFromPayoutLine(payout);
		av.remapAccountType(AccountingType.LIQUID_ASSET_AR, modeOfPaymentAccount);
		av.remapAccountType(AccountingType.OTHER_EXPENSES_SALES, feeGlAccount);
		av.remapAccountType(AccountingType.LIQUID_ASSET_CASH, intransitAccount);
		av.balanceWithLine(AccountingType.ROUNDING);
		
		Voucher fortnoxVoucher = fortnoxConverter.mapFromBusinessObjectVoucher(extendedClient.getCurrentFortnoxAdapter(), voucherSeries, av);
		
		if (!dryRun) {
			extendedClient.accountFortnoxVoucher(fortnoxVoucher, payout.getCurrency(), payout.getCurrencyRateToAccountingCurrency());
		} else {
			Log.info("Would have accounted voucher: " + fortnoxVoucher.toString());
		}
		
	}
	
	
	/**
	 * Process all applicable payments.
	 * 
	 * @throws Exception
	 */
	public void processPayments() throws Exception {
		
		if (processOptions.isAccountPayoutOnly() || processOptions.isAccountFeesOnly()) {
			return;
		}
		
		// Iterate through the payments
		if (!processOptions.isOnlyTrxNumber()) {
			for (Payment<?> payment : getPaymentBatch().getPayments()) {
				processPayment(payment);
			}
		}
		
	}

	private void processPayment(Payment<?> payment) throws Exception {
		Invoice inv;
		PaymentProcessResult paymentResult;
		
		if (processOptions.hasManualReferenceMap()) {
			String overrideReference = processOptions.getManualReferenceFor(payment.getDestinationSystemReference());
			if (overrideReference!=null) {
				payment.setMatchedInvoiceNo(overrideReference);
			}
		}
		
		inv = getInvoiceToPayAndUpdatePayment(payment);
		if (inv!=null) {
			paymentResult = payInvoice(inv, payment);
		} else if (processOptions.isNonMatchedAsPrepayments()) { 
			paymentResult = createUnmatchedPrepayment(payment);
		} else {
			paymentResult = new PaymentProcessResult(PaymentProcessResult.ResultCode.NOT_PROCESSED);
		}
		if (paymentResult.getException()!=null) {
			Log.error(paymentResult.getException().getMessage());
		}
		getPaymentBatchProcessResult().addPaymentProcessResult(paymentResult);
		
	}
	
	/**
	 * Creates an unmatched payment.
	 * 
	 * @param payment
	 * @return
	 * @throws Exception 
	 */
	private PaymentProcessResult createUnmatchedPrepayment(Payment<?> payment) throws Exception {
		
		AccountingVoucher av = AccountingVoucher.buildVoucherFromPayment(payment, 
				!processOptions.isAccountFeesOnly()  // Ignore write-offs.
				);
		
		av.remapAccountType(AccountingType.LIQUID_ASSET_AR, modeOfPrepaymentAccount);
		av.remapAccountType(AccountingType.OTHER_EXPENSES_SALES, feeGlAccount);
		av.remapAccountType(AccountingType.LIQUID_ASSET_CASH, modeOfPaymentAccount);
		av.balanceWithLine(AccountingType.ROUNDING);
		
		Voucher fortnoxVoucher = fortnoxConverter.mapFromBusinessObjectVoucher(extendedClient.getCurrentFortnoxAdapter(), voucherSeries, av);
		
		if (!dryRun) {
			extendedClient.accountFortnoxVoucher(fortnoxVoucher, payment.getCurrency(), FortnoxConstants.GET_RATE_FROM_FORTNOX);
		} else {
			Log.info("Would have accounted voucher: " + fortnoxVoucher.toString());
		}
		
		PaymentProcessResult result = new PaymentProcessResult(PaymentProcessResult.ResultCode.OK_WITH_WARNING);
		return result;
	}
	
	/**
	 * Pays the invoice using the attributes for modeOfPayment* and modeOfPrepayment*.
	 * 
	 * @param inv
	 * @param payment
	 * @return		A payment process result.
	 * @throws Exception
	 */
	public PaymentProcessResult payInvoice(Invoice inv, Payment<?> payment) throws Exception {

		if (inv==null || payment==null) {
			return new PaymentProcessResult(ResultCode.NOT_PROCESSED);
		}
		
		boolean bookkeepPayment = !processOptions.isDraftPaymentsIfPossible();
		
		prepareWriteOffs(payment);
		
		InvoicePayment invoicePayment = null;
		Exception paymentException = null;
		
		try {
			invoicePayment = extendedClient.payCustomerInvoice(
					modeOfPayment, 
					modeOfPrepayment,
					voucherSeries,
					inv, 
					bookkeepPayment, 
					processOptions.isFeesPerPayment(), 
					payment,
					dryRun);
		} catch (Exception ee) {
			paymentException = ee;
		}
		
		if (invoicePayment!=null && invoicePayment.getNumber()!=null && invoicePayment.getNumber()>0) {
			PaymentProcessResult ppr = new PaymentProcessResult(ResultCode.OK);
			ppr.setResultingPayment(FortnoxConverter.updatePaymentFromInvoicePayment(invoicePayment, payment));
			return ppr;
		} else {
			PaymentProcessResult ppr = new PaymentProcessResult(ResultCode.FAILED);
			if (paymentException!=null) {
				ppr.setException(paymentException);
				ppr.setTextResultFromException();
			}
			return ppr;
		}
		
	}
	
	public PaymentBatchProcessResult getPaymentBatchProcessResult() {
		return paymentBatchProcessResult;
	}

	/**
	 * If the fees are to be accounted per payment, they are added as write-offs.
	 * 
	 * @param payment
	 * @throws Exception
	 */
	private void prepareWriteOffs(Payment<?> payment) throws Exception {
		
		if (payment.hasPaymentWriteOffs()) {
			if (!processOptions.isFeesPerPayment()) {
				return;
			}
			
			for (PaymentWriteOff pwo : payment.getPaymentWriteOffs().getPaymentWriteOff()) {
				if (feeGlAccount!=null && !isAccountActive(pwo.getAccountNo())) {
					pwo.setAccountNo(feeGlAccount);
				}
			}
			// reverse the write-offs to the same account as the mode of payment
			PaymentWriteOff feeWriteOff = new PaymentWriteOff();
			feeWriteOff.setAccountNo(modeOfPaymentAccount);
			feeWriteOff.setAmount(payment.getAmount()-payment.getOriginalAmount());
			payment.addPaymentWriteOff(feeWriteOff);
			payment.setAmount(payment.getOriginalAmount());
			
		}
		
	}
	
	
	public Invoice getFortnoxInvoice(String invoiceNo) throws Exception {
		return extendedClient.getFortnoxInvoice(invoiceNo);
	}
	
 	public Invoice getInvoiceToPayAndUpdatePayment(Payment<?> payment) throws Exception {
		
 		if (payment.hasDestinationSystemReferenceRegex()) {
 			extendedClient.setReferenceRegex(payment.getDestinationSystemReferenceRegex());
 		}
 		
 		Invoice inv = null;
 		// If the invoice is already matched, lookup that invoice
 		if (payment.getMatchedInvoiceNo()!=null) {
 			inv = extendedClient.getFortnoxInvoice(payment.getMatchedInvoiceNo());
 		}
 		
 		if (inv==null) {
			inv = extendedClient.getInvoiceToPay(
			payment.getDestinationSystemReference(), 
			ReferenceField.valueOf(payment.getDestinationSystemReferenceField()), 
			payment.getPaymentDate(),
			null);
 		}
		if (inv!=null) {
			if (inv.getOCR()!=null && inv.getOCR().trim().length()>0)
				payment.setMatchedInvoiceNo(inv.getOCR());
			else 
				payment.setMatchedInvoiceNo(inv.getDocumentNumber());
			
			payment.setMatchedInvoiceOpenAmount(inv.getBalance());
			
			if (payment.getPayerName()==null || payment.getPayerName().trim().length()==0) {
				BusinessPartner<?> bp = payment.getBusinessPartner();
				if (bp==null) {
					bp = new BusinessPartner<Object>();
					payment.setBusinessPartner(bp);
				}
				bp.setName(inv.getCustomerName());
			}
		}
		return inv;
		
	}


	public void setOptions(PaymentBatchProcessOptions options) {
		processOptions = options;
		dryRun = processOptions.isDryRun();
	}


	public PaymentBatch getPaymentBatch() {
		return paymentBatch;
	}

	
	private void determineAccountingSettings() {
		if (paymentBatch!=null) {
			if (paymentBatch.getBankAccount()!=null) {
				intransitAccount = paymentBatch.getBankAccount().getGeneralLedgerInTransitAccount();
			} else {
				intransitAccount = null;
			}
			voucherSeries = paymentBatch.getVoucherSeries();
		}
	}
	
	private void refreshAccountMap() throws Exception {
		acctMap = extendedClient.getCurrentFortnoxClient().getAccountMap(paymentBatch.getFirstPaymentDate());
	}
	
	
	private void determineFeeAccount() throws Exception {
		
		if (paymentBatch==null || paymentBatch.getBankAccount()==null) {
			throw new UnableToDetermineFeeAccountException();
		}
		
		String feeAccount = paymentBatch.getBankAccount().getGeneralLedgerFeeAccount();
		if (feeAccount==null) {
			throw new UnableToDetermineFeeAccountException("No general ledger fee account defined for bank account.");
		}
		
		if (!isAccountActive(feeAccount)) {
			throw new UnableToDetermineFeeAccountException(feeAccount + " doesn't exist or is not active.");
		}
		
		feeGlAccount = feeAccount;
		
	}
	
	private boolean isAccountActive(String accountNo) throws Exception {
		if (accountNo==null) return false;
		if (acctMap==null)
			refreshAccountMap();
		AccountSubset as = acctMap.get(accountNo);
		if (as==null) return false;
		return as.getActive();
	}
	
	private void determineModeOfPayment() throws Exception {
		
		if (paymentBatch==null || paymentBatch.getBankAccount()==null) {
			throw new UnableToDetermineModeOfPaymentException("No bank account details in payment batch");
		}
		
		String paymentAccount = paymentBatch.getBankAccount().getGeneralLedgerAccountToUseForPayments();
		
		if (paymentAccount==null || paymentAccount.trim().length()==0) {
			throw new UnableToDetermineModeOfPaymentException("No general ledger payment account defined for bank account");
		}
		
		ModeOfPaymentSubset mps = mapAccountNoToModeOfPayment(paymentAccount); 
		modeOfPayment = mps.getCode();
		modeOfPaymentAccount = mps.getAccountNumber();
		
		String unmatchedPaymentAccount = paymentBatch.getGeneralLedgerUnknownTrxAccount();
		if (unmatchedPaymentAccount!=null) {
			modeOfPrepayment = mapAccountNoToModeOfPayment(unmatchedPaymentAccount).getCode();
			modeOfPrepaymentAccount = unmatchedPaymentAccount;
		}
		
	}
	
	
	private ModeOfPaymentSubset	mapAccountNoToModeOfPayment(String paymentAccount) throws Exception {

		FortnoxClient3 fortnoxClient = extendedClient.getCurrentFortnoxClient();
		ModesOfPayments modesOfPayments = fortnoxClient.getModesOfPayments();
		if (modesOfPayments==null) return null;
		List<ModeOfPaymentSubset> subsets = modesOfPayments.getModeOfPaymentSubset();
		
		ModeOfPaymentSubset result = null;
		
		for (ModeOfPaymentSubset subset : subsets) {
			if (paymentAccount.equals(subset.getAccountNumber())) {
				result = subset;
				break;
			}
		}
		
		if (result==null) {
			throw new UnableToDetermineModeOfPaymentException(paymentAccount + " can't be mapped to an existing mode of payment.");
		}
		
		return result;
		
	}
	
	
}
