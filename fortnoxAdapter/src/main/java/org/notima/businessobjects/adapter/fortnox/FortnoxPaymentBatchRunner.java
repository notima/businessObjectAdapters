package org.notima.businessobjects.adapter.fortnox;

import java.util.List;
import java.util.Map;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.AccountSubset;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.api.fortnox.entities3.ModeOfPaymentSubset;
import org.notima.api.fortnox.entities3.ModesOfPayments;
import org.notima.businessobjects.adapter.fortnox.FortnoxExtendedClient.ReferenceField;
import org.notima.businessobjects.adapter.fortnox.exception.UnableToDetermineFeeAccountException;
import org.notima.businessobjects.adapter.fortnox.exception.UnableToDetermineModeOfPaymentException;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchProcessOptions;
import org.notima.generic.businessobjects.PaymentProcessResult;
import org.notima.generic.businessobjects.PaymentProcessResult.ResultCode;
import org.notima.generic.businessobjects.PaymentWriteOff;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;

public class FortnoxPaymentBatchRunner {

	private FortnoxExtendedClient	cl;	
	private TaxSubjectIdentifier	taxSubject;
	private PaymentBatch			paymentBatch;
	private String					modeOfPayment;
	private String					feeGlAccount;
	
	private Map<String, AccountSubset> acctMap;
	
	private PaymentBatchProcessOptions	processOptions = new PaymentBatchProcessOptions();
	
	public FortnoxPaymentBatchRunner(FortnoxExtendedClient client) throws Exception {
		cl = client;
		CompanySetting cs = cl.getCompanySetting();
		taxSubject = new TaxSubjectIdentifier(cs.getOrganizationNumber(), cs.getCountryCode());
	}
	
	public PaymentProcessResult payInvoice(Invoice inv, Payment<?> payment) throws Exception {

		if (inv==null || payment==null) {
			return new PaymentProcessResult(ResultCode.NOT_PROCESSED);
		}
		
		boolean bookkeepPayment = !processOptions.isDraftPaymentsIfPossible();

		// Remap to default fee account if the account supplied for write offs doesn't exist
		mapFeeAccount(payment);
		
		InvoicePayment invoicePayment = cl.payCustomerInvoice(modeOfPayment, inv, bookkeepPayment, payment);
		
		if (invoicePayment!=null && invoicePayment.getNumber()>0) {
			return new PaymentProcessResult(ResultCode.OK);
		} else {
			return new PaymentProcessResult(ResultCode.FAILED);
		}
		
	}
	
	private void mapFeeAccount(Payment<?> payment) throws Exception {
		
		if (payment.getPaymentWriteOffs()!=null && payment.getPaymentWriteOffs().getPaymentWriteOff().size()>0) {
			for (PaymentWriteOff pwo : payment.getPaymentWriteOffs().getPaymentWriteOff()) {
				if (!isAccountActive(pwo.getAccountNo())) {
					pwo.setAccountNo(feeGlAccount);
				}
			}
		}
		
	}
	
	
	public Invoice getFortnoxInvoice(String invoiceNo) throws Exception {
		return cl.getFortnoxInvoice(invoiceNo);
	}
	
 	public Invoice getInvoiceToPayAndUpdatePayment(Payment<?> payment) throws Exception {
		
		Invoice inv = cl.getInvoiceToPay(
				payment.getDestinationSystemReference(), 
				ReferenceField.valueOf(payment.getDestinationSystemReferenceField()), 
				payment.getPaymentDate(),
				null);
		if (inv!=null) {
			if (inv.getOCR()!=null && inv.getOCR().trim().length()>0)
				payment.setMatchedInvoiceNo(inv.getOCR());
			else 
				payment.setMatchedInvoiceNo(inv.getDocumentNumber());
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
	}


	public PaymentBatch getPaymentBatch() {
		return paymentBatch;
	}


	public void setPaymentBatch(PaymentBatch paymentBatch) throws Exception {
		this.paymentBatch = paymentBatch;
		// TODO - Compare tax subject
		determineModeOfPayment();
		
		// Check if account map needs to be updated
		if (acctMap==null) {
			refreshAccountMap();
		}
		
		determineFeeAccount();
		
	}
	
	private void refreshAccountMap() throws Exception {
		acctMap = cl.getCurrentFortnoxClient().getAccountMap(paymentBatch.getFirstPaymentDate());
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

		modeOfPayment = mapAccountNoToModeOfPayment(paymentAccount);
		
	}
	
	
	private String	mapAccountNoToModeOfPayment(String paymentAccount) throws Exception {

		FortnoxClient3 fortnoxClient = cl.getCurrentFortnoxClient();
		ModesOfPayments modesOfPayments = fortnoxClient.getModesOfPayments();
		if (modesOfPayments==null) return null;
		List<ModeOfPaymentSubset> subsets = modesOfPayments.getModeOfPaymentSubset();
		
		String result = null;
		
		for (ModeOfPaymentSubset subset : subsets) {
			if (paymentAccount.equals(subset.getAccountNumber())) {
				result = subset.getCode();
				break;
			}
		}
		
		if (result==null) {
			throw new UnableToDetermineModeOfPaymentException(paymentAccount + " can't be mapped to an existing mode of payment.");
		}
		
		return result;
		
	}
	
	
}
