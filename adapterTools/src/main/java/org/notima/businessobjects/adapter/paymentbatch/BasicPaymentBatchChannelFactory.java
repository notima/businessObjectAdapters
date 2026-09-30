package org.notima.businessobjects.adapter.paymentbatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelList;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.util.LocalDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This is the common code for all payment batch channel factories. 
 * Implementations of this abstract class should focus on how to store the channel information.
 * 
 */
public abstract class BasicPaymentBatchChannelFactory implements PaymentBatchChannelFactory {

	private static final Logger log = LoggerFactory.getLogger(BasicPaymentBatchChannelFactory.class);

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
				refreshUnprocessedEntries(ch);
			}
		}
		
		return list;
		
	}

	/**
	 * Reads the channel's source directory and updates the channel's unprocessed entries 
	 * (one per source file) and the date range of their payments.
	 * 
	 * The result is only kept in memory. Call this whenever current information is needed.
	 * 
	 * @param pbc	The channel to refresh.
	 * @return	The channel.
	 */
	public PaymentBatchChannel refreshUnprocessedEntries(PaymentBatchChannel pbc) {

		List<String> entries = new ArrayList<String>();
		LocalDate fromDate = null;
		LocalDate untilDate = null;
		
		if (cof!=null && pbc.getOptions()!=null && pbc.getOptions().getSourceDirectory()!=null) {

			PaymentBatchFactory paymentFactory = cof.lookupPaymentBatchFactory(pbc.getSourceSystem());

			try {
				paymentFactory.setSource(pbc.getOptions().getSourceDirectory());

				List<PaymentBatch> batches = paymentFactory.readPaymentBatches(); 
				
				for (PaymentBatch b : batches) {
					// A file can result in several batches (ie one per currency)
					if (b.getSource()!=null && !entries.contains(b.getSource())) {
						entries.add(b.getSource());
					}
					if (b.getPayments()==null) continue;
					for (Payment<?> p : b.getPayments()) {
						LocalDate d = LocalDateUtils.asLocalDate(p.getPaymentDate());
						if (d==null) continue;
						if (fromDate==null || d.isBefore(fromDate)) {
							fromDate = d;
						}
						if (untilDate==null || d.isAfter(untilDate)) {
							untilDate = d;
						}
					}
				}
			} catch (Exception ee) {
				log.warn("Unable to read unprocessed entries for channel {}: {}", pbc.getChannelId(), ee.getMessage());
			}
			
		}
		
		pbc.setUnprocessedEntries(entries);
		pbc.setUnprocessedFromDate(fromDate);
		pbc.setUnprocessedUntilDate(untilDate);
		
		return pbc;
	}
	
	/**
	 * @deprecated	Use {@link #refreshUnprocessedEntries(PaymentBatchChannel)}
	 */
	@Deprecated
	protected PaymentBatchChannel populateUnProcessedEntries(PaymentBatchChannel pbc) {
		return refreshUnprocessedEntries(pbc);
	}
	
}
