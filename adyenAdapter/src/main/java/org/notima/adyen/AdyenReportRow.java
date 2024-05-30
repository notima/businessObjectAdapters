package org.notima.adyen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.notima.generic.ifacebusinessobjects.PaymentReportRow;

public class AdyenReportRow implements PaymentReportRow {

	public static final String	SETTLED_TYPE = "Settled";
	public static final String	FEE_TYPE = "Fee";
	public static final String	PAYOUT_TYPE = "MerchantPayout";
	public static final String  DEPOSIT_CORRECTION = "DepositCorrection";
	
	  // Initialize the static set using a static block
    public static final Set<String> TYPES;
    static {
        Set<String> tempSet = new HashSet<>();
        tempSet.add(SETTLED_TYPE);
        tempSet.add(FEE_TYPE);
        tempSet.add(PAYOUT_TYPE);
        TYPES = Collections.unmodifiableSet(tempSet);
    }
    
    private String	companyAccount;
    private String	merchantAccount;
    private String	pspReference;
    private String	merchantReference;
    private String	paymentMethod;
    private Date	creationDate;
    private String	timeZone;
    private String	lineType;
    private String	modificationReference;
    private String	grossCurrency;
    private Double	grossDebit;
    private Double	grossCredit;
    private Double	exchangeRate;
    private String	netCurrency;
    private Double	netDebit;
    private Double	netCredit;
    private Double	commission;
    private Double	markup;
    private Double	schemeFees;
    private Double	interchange;
    private String	paymentMethodVariant;
    private String	modificationMerchantReference;
    private Integer batchNumber;
    private String	reserved4;
    private String	reserved5;
    private String	reserved6;
    private String	reserved7;
    private String	reserved8;
    private String	reserved9;
    private String	reserved10;
    
    private List<AdyenFee>	fees;

	public String getCompanyAccount() {
		return companyAccount;
	}

	public void setCompanyAccount(String companyAccount) {
		this.companyAccount = companyAccount;
	}


	public String getMerchantAccount() {
		return merchantAccount;
	}


	public void setMerchantAccount(String merchantAccount) {
		this.merchantAccount = merchantAccount;
	}


	public String getPspReference() {
		return pspReference;
	}


	public void setPspReference(String pspReference) {
		this.pspReference = pspReference;
	}


	public String getMerchantReference() {
		return merchantReference;
	}


	public void setMerchantReference(String merchantReference) {
		this.merchantReference = merchantReference;
	}


	public String getPaymentMethod() {
		return paymentMethod;
	}


	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}


	public Date getCreationDate() {
		return creationDate;
	}


	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}


	public String getTimeZone() {
		return timeZone;
	}


	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}


	public String getLineType() {
		return lineType;
	}


	public void setLineType(String lineType) {
		this.lineType = lineType;
	}


	public String getModificationReference() {
		return modificationReference;
	}


	public void setModificationReference(String modificationReference) {
		this.modificationReference = modificationReference;
	}

	public String getGrossCurrency() {
		return grossCurrency;
	}

	@Override
	public String getCurrency() {
		return (getNetCurrency());
	}
	
	public void setGrossCurrency(String grossCurrency) {
		this.grossCurrency = grossCurrency;
	}

	public Double getOriginalAmount() {
		return getGrossCredit() - getGrossDebit(); 
	}
	
	public Double getGrossDebit() {
		return grossDebit!=null ? grossDebit : Double.valueOf(0);
	}

	public void setGrossDebit(Double grossDebit) {
		this.grossDebit = grossDebit;
	}

	public Double getGrossCredit() {
		return grossCredit!=null ? grossCredit : Double.valueOf(0);
	}

	public void setGrossCredit(Double grossCredit) {
		this.grossCredit = grossCredit;
	}


	public Double getExchangeRate() {
		return exchangeRate;
	}


	public void setExchangeRate(Double exchangeRate) {
		this.exchangeRate = exchangeRate;
	}


	public String getNetCurrency() {
		return netCurrency;
	}


	public void setNetCurrency(String netCurrency) {
		this.netCurrency = netCurrency;
	}

	public Double getAmount() {
		return getNetCredit() - getNetDebit();
	}

	public Double getNetDebit() {
		return netDebit!=null ? netDebit : Double.valueOf(0);
	}

	public void setNetDebit(Double netDebit) {
		this.netDebit = netDebit;
	}


	public Double getNetCredit() {
		return netCredit!=null ? netCredit : Double.valueOf(0);
	}


	public void setNetCredit(Double netCredit) {
		this.netCredit = netCredit;
	}


	public Double getCommission() {
		return commission!=null ? commission : Double.valueOf(0);
	}


	public void setCommission(Double commission) {
		this.commission = commission;
	}


	public Double getMarkup() {
		return markup!=null ? markup : Double.valueOf(0);
	}


	public void setMarkup(Double markup) {
		this.markup = markup;
	}


	public Double getSchemeFees() {
		return schemeFees!=null ? schemeFees : Double.valueOf(0);
	}


	public void setSchemeFees(Double schemeFees) {
		this.schemeFees = schemeFees;
	}


	public Double getInterchange() {
		return interchange!=null ? interchange : Double.valueOf(0);
	}


	public void setInterchange(Double interchange) {
		this.interchange = interchange;
	}


	public String getPaymentMethodVariant() {
		return paymentMethodVariant;
	}


	public void setPaymentMethodVariant(String paymentMethodVariant) {
		this.paymentMethodVariant = paymentMethodVariant;
	}


	public String getModificationMerchantReference() {
		return modificationMerchantReference;
	}


	public void setModificationMerchantReference(String modificationMerchantReference) {
		this.modificationMerchantReference = modificationMerchantReference;
	}


	public Integer getBatchNumber() {
		return batchNumber;
	}


	public void setBatchNumber(Integer batchNumber) {
		this.batchNumber = batchNumber;
	}


	public String getReserved4() {
		return reserved4;
	}


	public void setReserved4(String reserved4) {
		this.reserved4 = reserved4;
	}


	public String getReserved5() {
		return reserved5;
	}


	public void setReserved5(String reserved5) {
		this.reserved5 = reserved5;
	}


	public String getReserved6() {
		return reserved6;
	}


	public void setReserved6(String reserved6) {
		this.reserved6 = reserved6;
	}


	public String getReserved7() {
		return reserved7;
	}


	public void setReserved7(String reserved7) {
		this.reserved7 = reserved7;
	}


	public String getReserved8() {
		return reserved8;
	}


	public void setReserved8(String reserved8) {
		this.reserved8 = reserved8;
	}


	public String getReserved9() {
		return reserved9;
	}


	public void setReserved9(String reserved9) {
		this.reserved9 = reserved9;
	}


	public String getReserved10() {
		return reserved10;
	}


	public void setReserved10(String reserved10) {
		this.reserved10 = reserved10;
	}


	public List<AdyenFee> getFees() {
		return fees;
	}


	public void setFees(List<AdyenFee> fees) {
		this.fees = fees;
	}

	@Override
	public boolean hasPaymentReference() {
		return pspReference!=null && pspReference.trim().length()>0;
	}
	
	@Override
	public boolean isPayment() {
		return hasPaymentReference() && SETTLED_TYPE.equals(lineType);
	}

	@Override
	public boolean isDepositAdjustment() {
		return DEPOSIT_CORRECTION.equals(lineType);
	}
	
	@Override
	public boolean isFee() {
		return FEE_TYPE.equals(lineType);
	}

	@Override
	public boolean isPayout() {
		return PAYOUT_TYPE.equals(lineType);
	}

	@Override
	public String getPaymentReference() {
		return pspReference;
	}

	public void processFees() {
		fees = new ArrayList<AdyenFee>();
	
		if (getCommission()!=0) {
			addFee(new AdyenFee(AdyenFee.COMMISSION, getCommission(), "Commission"));
		}
		if (getMarkup()!=0) {
			addFee(new AdyenFee(AdyenFee.MARKUP, getMarkup(), "Markup"));
		}
		if (getSchemeFees()!=0) {
			addFee(new AdyenFee(AdyenFee.SCHEME, getInterchange(), "Scheme fees"));
		}
		if (getInterchange()!=0) {
			addFee(new AdyenFee(AdyenFee.INTERCHANGE, getInterchange(), "Interchange"));
		}
	}
	
	private void addFee(AdyenFee fee) {
		fees.add(fee);
	}
	
}
