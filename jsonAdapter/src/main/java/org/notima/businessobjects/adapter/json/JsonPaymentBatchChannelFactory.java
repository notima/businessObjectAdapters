package org.notima.businessobjects.adapter.json;

import java.util.List;

import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;

public class JsonPaymentBatchChannelFactory implements PaymentBatchChannelFactory {

	@Override
	public List<PaymentBatchChannel> listChannelsForTenant(TaxSubjectIdentifier tenant) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PaymentBatchChannel> listChannelsWithSourceSystem(String systemName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PaymentBatchChannel> listChannelsWithDestinationSystem(String systemName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaymentBatchChannel persistChannel(PaymentBatchChannel pbc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSystemName() {
		return JsonAdapter.SYSTEM_NAME;
	}

}
