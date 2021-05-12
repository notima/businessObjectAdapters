package org.notima.businessobjects.adapter.tools.table;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.notima.generic.businessobjects.BalanceSheetColumn;
import org.notima.generic.businessobjects.BalanceSheetLine;
import org.notima.generic.businessobjects.BalanceSheetReport;

public class BalanceSheetTable extends GenericTable {
	
	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public BalanceSheetTable(BalanceSheetReport plr) {
		
		addColumn("AcctNo");
		addColumn("Description");
		addColumn("Amount", GenericColumn.ALIGNMENT_RIGHT);
		
		if (plr==null || plr.getLines()==null || plr.getLines().size()==0) {
			setEmptyTableText("No content");
			return;
		}

		BalanceSheetColumn plc;
		
		BigDecimal result = BigDecimal.ZERO;
		
		for (BalanceSheetLine pll : plr.getLines()) {

			plc = pll.getColumns().get(0);
			if (plc!=null && plc.getAmount().signum()!=0) {
				
				result = result.add(plc.getAmount());
				
				addRow().addContent(
						pll.getAccountNo(), 
						pll.getDescription(),
						nfmt.format(pll.getColumns().get(0).getAmount().negate())
						);
				
			}
			
		}
		
		if (result.signum()!=0) {
			addRow().addContent("", "CALCULATED RESULT", nfmt.format(result));
		}
		
	}
	
	
	
}
