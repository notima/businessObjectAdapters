package org.notima.businessobjects.adapter.tools.table;

import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;

public class PaymentChannelStatusTable extends GenericTable {

	private PaymentBatchChannel ch;
	
	public PaymentChannelStatusTable(PaymentBatchChannel bpl) {

		addColumn("Key");
		addColumn("Value");
		
		if (bpl==null) {
			setEmptyTableText("No such channel");
			return;
		}
		
		ch = bpl;
		
		populateRows();
		
	}

	private void populateRows() {
		
		if (getRows()!=null)
			this.getRows().clear();

		if (ch==null) return;
		
		addRow().addContent("ChannelID", ch.getChannelId());
		addRow().addContent("Tenant", getTenantName());
		addRow().addContent("Flow", getFlow());
		addRow().addContent("Source dir", getSourceDirectory());
		addRow().addContent("Last batch", getLastBatch());
		addRow().addContent("R. until", getReconciledUntilString());
		
		int fileCounter = 1;
		for (String s : ch.getUnprocessedEntries()) {
			addRow().addContent(Integer.toString(fileCounter), s);
			fileCounter++;
		}
		
	}

	private String getFlow() {
		return ch.getSourceSystem() + " -> " + ch.getDestinationSystem();
	}
	
	private String getTenantName() {
		TaxSubjectIdentifier tsi = ch.getTenant();
		if (tsi==null) return "-";
		return tsi.toString();
	}
	
	private String getSourceDirectory() {
		StringBuffer str = new StringBuffer();
		if (ch.getOptions()!=null && ch.getOptions().getSourceDirectory()!=null) {
			str.append(ch.getOptions().getSourceDirectory());
		}
		if (ch.getUnprocessedEntries()!=null && ch.getUnprocessedEntries().size()>0) {
			if (str.length()>0) {
				str.append(" ");
			}
			str.append("(" + ch.getUnprocessedEntries().size() + ")");
		}
		return str.toString();
	}
	
	private String getLastBatch() {
		if (ch.getStatus()!=null && ch.getStatus().getLastProcessedBatch()!=null) {
			return ch.getStatus().getLastProcessedBatch();
		}
		return "-";
	}
	
	private String getReconciledUntilString() {
		
		if (ch.getStatus()!=null && ch.getStatus().getReconciledUntil()!=null) {
			return ch.getStatus().getReconciledUntil().toString();
		}
		
		return "";
	}
	
}
