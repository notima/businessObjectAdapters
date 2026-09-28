package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;

public class PaymentBatchChannelTable extends GenericTable {

	private List<PaymentBatchChannel> list;
	private boolean showActiveFlag;
	
	public PaymentBatchChannelTable(List<PaymentBatchChannel> bpl) {
		this(bpl, false);
	}
	
	/**
	 * 
	 * @param bpl				The channels to show.
	 * @param showActiveFlag	If true, a column flagging inactive channels is added.
	 */
	public PaymentBatchChannelTable(List<PaymentBatchChannel> bpl, boolean showActiveFlag) {

		this.showActiveFlag = showActiveFlag;

		addColumn("ChannelID");
		addColumn("Tenant");
		addColumn("Source");
		addColumn("Destination");
		addColumn("Description");
		addColumn("Source dir");
		addColumn("R. until");
		if (showActiveFlag) {
			addColumn("Active");
		}
		
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
			GenericRow row = addRow();
			row.addContent(
					p.getChannelId(), 
					p.getTenant().toString(), 
					p.getSourceSystem(),
					p.getDestinationSystem(),
					p.getChannelDescription(),
					getSourceDirectory(p),
					getReconciledUntilString(p)
					);
			if (showActiveFlag) {
				row.addContent(isActive(p) ? "Yes" : "NO");
			}
		}
		
	}

	/**
	 * A channel without status is considered active.
	 */
	public static boolean isActive(PaymentBatchChannel p) {
		return p.getStatus()==null || p.getStatus().isActive();
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
