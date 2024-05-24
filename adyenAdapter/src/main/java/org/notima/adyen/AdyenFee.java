package org.notima.adyen;

public class AdyenFee {

	private Integer feeType;
	private Double	amount;
	private String comment;
	
	public final static Integer	COMMISSION = 1;
	public final static Integer MARKUP = 2;
	public final static Integer SCHEME = 3;
	public final static Integer INTERCHANGE = 4;
	
	public AdyenFee() {};
	
	public AdyenFee(Integer feeType, Double amount, String comment) {
		this.feeType = feeType;
		this.amount = amount;
		this.comment = comment;
	}
	
	public Integer getFeeType() {
		return feeType;
	}
	public void setFeeType(Integer feeType) {
		this.feeType = feeType;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}

	
	
}
