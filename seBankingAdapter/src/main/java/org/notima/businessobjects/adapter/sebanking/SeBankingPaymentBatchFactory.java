package org.notima.businessobjects.adapter.sebanking;

import java.util.List;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchChannelOptions;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;

public class SeBankingPaymentBatchFactory implements PaymentBatchFactory {

	public static final String SYSTEMNAME = "SeBanking";
	
	@Override
	public void setSource(String source) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDestination(String dest) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<PaymentBatch> readPaymentBatches() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaymentBatch writePaymentBatch(PaymentBatch batch) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}

	@Override
	public PaymentBatchChannelOptions getChannelOptions() {
		// TODO Auto-generated method stub
		return null;
	}

}
