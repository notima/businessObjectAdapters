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
		addColumn("R. until");
		
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
					getSourceDirectory(p),
					getReconciledUntilString(p)
					);
		}
		
	}

	private String getSourceDirectory(PaymentBatchChannel p) {
		StringBuffer str = new StringBuffer();
		if (p.getOptions()!=null && p.getOptions().getSourceDirectory()!=null) {
			str.append(p.getOptions().getSourceDirectory());
		}
		if (p.getUnprocessedEntries()!=null && p.getUnprocessedEntries().size()>0) {
			if (str.length()>0) {
				str.append(" ");
			}
			str.append("(" + p.getUnprocessedEntries().size() + ")");
		}
		return str.toString();
	}
	
	private String getReconciledUntilString(PaymentBatchChannel p) {
		
		if (p.getStatus()!=null && p.getStatus().getReconciledUntil()!=null) {
			return p.getStatus().getReconciledUntil().toString();
		}
		
		return "";
	}
	
}
