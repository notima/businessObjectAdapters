package org.notima.businessobjects.adapter.paymentbatch;

import java.util.ArrayList;
import java.util.List;

import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelList;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;

/**
 * 
 * This is the common code for all payment batch channel factories. 
 * Implementations of this abstract class should focus on how to store the channel information.
 * 
 */
public abstract class BasicPaymentBatchChannelFactory implements PaymentBatchChannelFactory {

	protected CanonicalObjectFactory	cof;
	
	protected boolean populateUnProcessedEntries = false;
	
	protected PaymentBatchChannelList channelList;

	public void populateUnprocessedEntries(boolean flag) {
		populateUnProcessedEntries = flag;
	};
	
	public boolean isPopulateUnprocessedEntries() {
		return populateUnProcessedEntries;
	};
	
	public void setCanonicalObjectFactory(CanonicalObjectFactory c) {
		cof = c;
	}
	
	@Override
	public List<PaymentBatchChannel> listChannelsForTenant(TaxSubjectIdentifier tenant) {
		return populateIfNeeded(channelList.listChannelsForTenant(tenant));
	}

	private List<PaymentBatchChannel> populateIfNeeded(List<PaymentBatchChannel> list) {
		
		if (populateUnProcessedEntries) {
			for (PaymentBatchChannel ch : list) {
				populateUnProcessedEntries(ch);
			}
		}
		
		return list;
		
	}

	/**
	 * Looks for unprocessed entries for this given channel.
	 * 
	 * @param pbc
	 * @return
	 */
	protected PaymentBatchChannel populateUnProcessedEntries(PaymentBatchChannel pbc) {

		PaymentBatchFactory paymentFactory;
		
		if (cof!=null) {

			paymentFactory = cof.lookupPaymentBatchFactory(pbc.getSourceSystem());

			try {
				paymentFactory.setSource(pbc.getOptions().getSourceDirectory());

				List<PaymentBatch> batches = paymentFactory.readPaymentBatches(); 
				
				List<String> entries = new ArrayList<String>();
				for (PaymentBatch b : batches) {
					entries.add(b.getSource());
				}
				pbc.setUnprocessedEntries(entries);
			} catch (Exception ee) {
				
			}
			
		}
		
		return pbc;
	}
	
}
