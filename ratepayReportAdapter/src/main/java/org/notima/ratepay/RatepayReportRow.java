package org.notima.ratepay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RatepayReportRow {

    public static final int B2B = 1;
    public static final int B2C = 2;

    public static final int	FEETYPE_PAYMENT = 1;
    public static final int FEETYPE_PROVISION = 2;
    public static final int FEETYPE_TRX_FEE = 3;
    
    public static final int PRODUCT_INVOICE = 1;
    public static final int PRODUCT_INSTALLMENT = 2;
    public static final int PRODUCT_DIRECT_DEBIT = 3;
    public static final int PRODUCT_PREPAYMENT = 4;
    public static final int PRODUCT_INSTALLMENT_DIRECT_DEBIT = 5;

    private String shopId;
    private Date paymentDate;
    private String shopName;
    private double amount;
    private String descriptor;
    private String shopInvoiceId;
    private String shopsOrderId;
    private String invoiceNumber;
    private String description;
    private int feeType;
    private Date orderDate;
    private Date sentDate;
    private String transactionId;
    private int customerGroup;
    private boolean knownCustomer;
    private int product;
    private int referenceIdAccounting;
    private String chargeBackReason;
    
    private List<RatepayFee>	fees;

    /**
     * @return Identifier of shop
     */
    public String getShopId() {
        return shopId;
    }
    
    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    /**
     * @return Date when payment was received by Ratepay-FiBu
     */
    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    /**
     * @return Name of shop
     */
    public String getShopName() {
        return shopName;
    }
    
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    /**
     * @return Amount of position
     */
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * @return Ratepay-reference of order
     */
    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public RatepayReportRow addFee(RatepayFee fee) {
    	if (fees==null) {
    		fees = new ArrayList<RatepayFee>();
    	}
    	fees.add(fee);
    	return this;
    }
    
    /**
     * @return Identifier of shop-Invoice
     */
    public String getShopInvoiceId() {
        return shopInvoiceId;
    }

    public void setShopInvoiceId(String shopInvoiceId) {
        this.shopInvoiceId = shopInvoiceId;
    }

    /**
     * @return Identifier of shop-order
     */
    public String getShopsOrderId() {
        return shopsOrderId;
    }

    public void setShopsOrderId(String orderId) {
        this.shopsOrderId = orderId;
    }

    /**
     * @return Identifier of Ratepay-Invoice
     */
    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    
    public List<RatepayFee> getFees() {
		return fees;
	}

	public void setFees(List<RatepayFee> fees) {
		this.fees = fees;
	}

	/**
     * @return Description of type of amount
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Merges information from another row to this row.
     * Merging is mostly done to cater for different types of amounts.
     * 
     * @param that
     * @return
     */
    public RatepayReportRow mergeRow(RatepayReportRow that) throws Exception {
    	if (this.descriptor!=null && !this.descriptor.equals(that.getDescriptor())) {
    		throw new Exception("Can't merge rows with different descriptors. Dst: " + this.descriptor + " Src: " + that.descriptor);
    	}
    	if (that.feeType==FEETYPE_PAYMENT) {
    		this.setAmount(this.getAmount() + that.getAmount());
    	} else {
    		this.addFee(new RatepayFee(new Integer(that.feeType), new Double(that.amount), that.description));
    	}
    	
    	return this;
    }
    
    /**
     * ID of type of amount
     * @return  1 - Original amount, return, cancellation, credit memo, additional debit
     *          2 - Disagio
     *          3 - Transaction fee (occur at cancellation or delivery)
     *          4 - Fees for return, cancellation, credit memo, additional debit
     *          5 - Service charge
     *          6 - VAT
     *          7 - Interest cost
     *          null - Other (monthly fee or similar)
     *          8 - Retransfer / Chargeback/Reassignment
     */
    public int getFeeType() {
        return feeType;
    }

    public void setFeeType(int feeType) {
        this.feeType = feeType;
    }

    /**
     * @return Date when request was sent to Ratepay-ga)teway
     */
    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    /**
     * @return Date when operation was sent to Ratepay-gateway
     */
    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    /**
     * @return Ratepay-Identifier of order
     */
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Customer Group Identifier
     * @return  1 - B2B
     *          2 - B2C
     */
    public int getCustomerGroup() {
        return customerGroup;
    }

    public void setCustomerGroup(int customerGroup) {
        this.customerGroup = customerGroup;
    }

    /**
     * Customer sent as known regular customer by shop
     * @return  1 - Known customer
     *          0 - Unknown customer
     */
    public boolean isKnownCustomer() {
        return knownCustomer;
    }

    public void setKnownCustomer(boolean knownCustomer) {
        this.knownCustomer = knownCustomer;
    }

    /**
     * ID describing product
     * @return  1 - INVOICE
     *          2 - INSTALLMENT
     *          3 - DIRECT DEBIT
     *          4 - PREPAYMENT
     *          5 - INSTALLMENT DIRECT DEBIT
     */
    public int getProduct() {
        return product;
    }

    public void setProduct(int product) {
        this.product = product;
    }

    /**
     * @return Shop internal accounting number (e. g. from ERP-system such as SAP), used to reference payment-changes
     */
    public int getReferenceIdAccounting() {
        return referenceIdAccounting;
    }

    public void setReferenceIdAccounting(int referenceIdAccounting) {
        this.referenceIdAccounting = referenceIdAccounting;
    }

    /**
     * Reason code for the chargeback at dispute process; provided only in case of fee type 8
     * @return  INR 
     *          SUM
     *          SNAD
     *          DEFEND
     *          FRAUD
     */
    public String getChargeBackReason() {
        return chargeBackReason;
    }

    public void setChargeBackReason(String chargeBackReason) {
        this.chargeBackReason = chargeBackReason;
    }

}
