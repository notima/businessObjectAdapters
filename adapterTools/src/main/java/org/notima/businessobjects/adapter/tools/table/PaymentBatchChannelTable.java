package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;

public class PaymentBatchChannelTable extends GenericTable {

	private List<PaymentBatchChannel> list;
	
	
	public PaymentBatchChannelTable(List<PaymentBatchChannel> bpl) {

		addColumn("ChannelID");
		addColumn("Tenant");
		addColumn("Destination");
		addColumn("Source");
		addColumn("Description");
		addColumn("Source dir");
		
		if (bpl==null || bpl.size()==0) {
			setEmptyTableText("No channels");
			return;
		}
		
		list = bpl;
		populateRows();
		
	}

	private void populateRows() {
		
		if (getRows()!=null)
			this.getRows().clear();

		if (list==null) return;
		
		for (PaymentBatchChannel p : list) {
			addRow().addContent(
					p.getChannelId(), 
					p.getTenant().toString(), 
					p.getDestinationSystem(),
					p.getSourceSystem(),
					p.getChannelDescription(),
					(p.getOptions()!=null ? p.getOptions().getSourceDirectory() : "")
					);
		}
		
	}
	
	
}
