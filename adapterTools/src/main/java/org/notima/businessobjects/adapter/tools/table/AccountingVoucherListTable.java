package org.notima.businessobjects.adapter.tools.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.notima.businessobjects.adapter.tools.table.GenericColumn;
import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;

public class AccountingVoucherListTable extends GenericTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	private boolean formatNumbers = false;
	
	public void initColumns() {
		
		addColumn("Date");
		addColumn("Acct No");
		addColumn("Description");
		GenericColumn col = new GenericColumn("Debet");
		col.alignRight();
		addColumn(col);
		col = new GenericColumn("Credit");
		col.alignRight();
		addColumn(col);
		addColumn("Is deleted");
		
	}
	
	public AccountingVoucherListTable(List<AccountingVoucher> vouchers, boolean formatNumbers) {
		initColumns();
		this.formatNumbers = formatNumbers;

		if (vouchers==null || vouchers.size()==0) {
			return;
		}
		for (AccountingVoucher vv : vouchers)
			addRow(vv);	
		
	}
	
	private void addRow(AccountingVoucher vv) {

		if (vv==null)
			return;
		
		if (vv.getLines()==null || vv.getLines().size()==0) {
			return;
		}

		// Add a first row
		addRow().addContent(
				(vv.getVoucherSeries() + " " + vv.getVoucherNo()),
				"",
				vv.getDescription(),
				vv.getCostCenter()!=null && vv.getCostCenter().trim().length()>0 ? ("Cst " + vv.getCostCenter()) : "",
				vv.getProjectCode()!=null && vv.getProjectCode().trim().length()>0 ? ("Pr " + vv.getProjectCode()) : "",
				""
				);
		
		for (AccountingVoucherLine vr : vv.getLines()) {
			addRow().addContent(
					vv.getAcctDate(),
					vr.getAcctNo(),
					vr.getAcctName() + " " + vr.getDescription(),
					formatNumbers ? nfmt.format(vr.getDebitAmount()) : vr.getDebitAmount(),
					formatNumbers ? nfmt.format(vr.getCreditAmount()) : vr.getCreditAmount(),
					vr.isDeleted()!=null && vr.isDeleted().booleanValue() ? "*" : ""
					)
					;
		}
		
		// Add comment if existing
		if (vv.getComments()!=null && vv.getComments().trim().length()>0) {
			addRow().addContent(
					"",
					"",
					vv.getComments(),
					"",
					"",
					""
					);
		}
		
		addEmptyRow();
		
	}
	
	private void addEmptyRow() {
		addRow().addContent(
				"---------",
				"",
				"",
				"",
				"",
				"");
	}
	
	
}
