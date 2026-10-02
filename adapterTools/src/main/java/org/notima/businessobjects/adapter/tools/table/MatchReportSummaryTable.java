package org.notima.businessobjects.adapter.tools.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map;

import org.notima.generic.businessobjects.ThresholdCheckResult;

/**
 * Summarizes the matching result of the channels in a match report run, one row per channel.
 * A channel with unmatched amounts in more than one currency gets an extra row for each additional currency.
 */
public class MatchReportSummaryTable extends GenericTable {

	private NumberFormat pctFmt = new DecimalFormat("0.00");

	public MatchReportSummaryTable() {

		addColumn("Channel");
		addColumn(new GenericColumn("Payments").alignRight());
		addColumn(new GenericColumn("Matched").alignRight());
		addColumn(new GenericColumn("Unmatched").alignRight());
		addColumn(new GenericColumn("Unmatched %").alignRight());
		addColumn("Currency");
		addColumn(new GenericColumn("Unmatched amt").alignRight());
		setEmptyTableText("No channels matched");

	}

	/**
	 * Adds a channel's matching result.
	 *
	 * @param channelName	The channel's name.
	 * @param total			The result over all the channel's pending files.
	 */
	public void addChannel(String channelName, ThresholdCheckResult total) {

		int payments = total.getPaymentCount();
		int unmatched = total.getUnmatchedCount();
		Iterator<Map.Entry<String, Double>> amounts = total.getUnmatchedAmountPerCurrency().entrySet().iterator();
		Map.Entry<String, Double> amount = amounts.hasNext() ? amounts.next() : null;

		addRow().addContent(
				channelName,
				numberCell(Integer.toString(payments), payments),
				numberCell(Integer.toString(payments - unmatched), payments - unmatched),
				numberCell(Integer.toString(unmatched), unmatched),
				payments>0 ? numberCell(pctFmt.format(total.getUnmatchedPercent()), Math.round(total.getUnmatchedPercent() * 100) / 100.0) : "",
				amount!=null ? amount.getKey() : "",
				amount!=null ? amountCell(amount.getValue()) : "");

		while (amounts.hasNext()) {
			amount = amounts.next();
			addRow().addContent("", "", "", "", "", amount.getKey(), amountCell(amount.getValue()));
		}

	}

	private GenericCell amountCell(double amount) {
		return numberCell(nfmt.format(amount), amount);
	}

	/**
	 * @return	A cell showing the formatted text, keeping the number for formats like Excel.
	 */
	private static GenericCell numberCell(String text, Number value) {
		GenericCell cell = new GenericCell(text);
		cell.setOriginalData(value);
		return cell;
	}

}
