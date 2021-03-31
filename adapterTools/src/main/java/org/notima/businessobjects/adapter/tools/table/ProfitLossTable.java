package org.notima.businessobjects.adapter.tools.table;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.generic.businessobjects.ProfitLossColumn;
import org.notima.generic.businessobjects.ProfitLossLine;
import org.notima.generic.businessobjects.ProfitLossReport;

public class ProfitLossTable extends ShellTable {
	
	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public ProfitLossTable(ProfitLossReport plr) {
		
		Col col = new Col("AcctNo");
		column(col);
		col = new Col("Description");
		column(col);
		col = new Col("Amount").alignRight();
		column(col);
		
		if (plr==null || plr.getLines()==null || plr.getLines().size()==0) {
			emptyTableText("No content");
			return;
		}

		ProfitLossColumn plc;
		
		BigDecimal result = BigDecimal.ZERO;
		
		for (ProfitLossLine pll : plr.getLines()) {

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
			addRow().addContent("", "CALCULATED RESULT", nfmt.format(result.negate()));
		}
		
	}
	
	
	
}
