package org.notima.businessobjects.adapter.json;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.notima.businessobjects.adapter.json.impl.PaymentBatchChannelListImpl;
import org.notima.businessobjects.adapter.paymentbatch.BasicPaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.util.IDGenerator;

public class JsonPaymentBatchChannelFactory extends BasicPaymentBatchChannelFactory {

	public JsonPaymentBatchChannelFactory(String pathToJsonFile) throws IOException {
		
		channelList = PaymentBatchChannelListImpl.createFromFile(new File(pathToJsonFile));
		
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
	public PaymentBatchChannel persistChannel(PaymentBatchChannel pbc) throws IOException {
		// Create new ID
		if (pbc.getChannelId()==null || pbc.getChannelId().trim().length()==0) {
			pbc.setChannelId(IDGenerator.generateID(10));
			while (findChannelWithId(pbc.getChannelId())!=null) {
				pbc.setChannelId(IDGenerator.generateID(10));
			}
			channelList.getChannelList().add(pbc);
		} else {
			PaymentBatchChannel existing = findChannelWithId(pbc.getChannelId());
			if (existing!=null) {
				replace(pbc, existing);
			} else {
				channelList.getChannelList().add(pbc);
			}
		}
		
		// Save to underlying file system.
		((PaymentBatchChannelListImpl)channelList).writeToFile();
		
		return pbc;
	}
	
	private void replace(PaymentBatchChannel newPbc, PaymentBatchChannel oldPbc) {
		channelList.getChannelList().remove(oldPbc);
		channelList.getChannelList().add(newPbc);
	}
	
	@Override
	public String getSystemName() {
		return JsonAdapter.SYSTEM_NAME;
	}

	@Override
	public PaymentBatchChannel findChannelWithId(String id) {
		if (id==null || channelList==null) return null;
		
		for (PaymentBatchChannel ch : channelList.getChannelList()) {
			if (id.equals(ch.getChannelId())) {
				return ch;
			}
		}
		
		return null;
	}

	@Override
	public PaymentBatchChannel findChannelByDescription(String desc) {
		
		if (desc==null || channelList==null) return null;
		
		for (PaymentBatchChannel ch : channelList.getChannelList()) {
			if (desc.equals(ch.getChannelDescription())) {
				return ch;
			}
		}
		
		return null;
		
	}
	
	@Override
	public List<PaymentBatchChannel> findChannelsBySource(String source) {

		List<PaymentBatchChannel> result = new ArrayList<PaymentBatchChannel>();
		
		if (source==null || channelList==null) return result;
		
		for (PaymentBatchChannel ch : channelList.getChannelList()) {
			if (source.equals(ch.getSourceSystem())) {
				result.add(ch);
			}
		}
		
		return result;
		
		
	}
	
}
