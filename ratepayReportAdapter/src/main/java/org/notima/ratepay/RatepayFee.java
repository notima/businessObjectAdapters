package org.notima.ratepay;

public class RatepayFee {

	private Integer feeType;
	private Double	amount;
	private String comment;
	
	public RatepayFee() {};
	
	public RatepayFee(Integer feeType, Double amount, String comment) {
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
