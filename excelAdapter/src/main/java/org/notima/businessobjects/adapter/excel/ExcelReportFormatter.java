package org.notima.businessobjects.adapter.excel;

import java.io.FileOutputStream;
import java.util.Date;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.ReportFormatter;
import org.notima.businessobjects.adapter.tools.table.GenericCell;
import org.notima.businessobjects.adapter.tools.table.GenericColumn;
import org.notima.businessobjects.adapter.tools.table.GenericRow;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class ExcelReportFormatter extends BasicReportFormatter implements ReportFormatter<GenericTable> {
	
	public static final String[] formats = new String[] {
		"xlsx",
		"xls",
		"ods"
	};
	
	@Override
	public String formatReport(GenericTable data, String format, Properties props) throws Exception {
		
		setFromProperties(props);
		
		Workbook wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet();
		
		Row row;
		Cell cell;
		int rowId = 0;
		int colId = 0;

		// Create first row
		row = sheet.createRow(rowId++);

		if (data.hasHeaders()) {
		
			Font headerFont = wb.createFont();
			headerFont.setBold(true);
			CellStyle headerStyle = wb.createCellStyle();
			headerStyle.setFont(headerFont);
			
			for (GenericColumn cc : data.getColumns()) {
				cell = row.createCell(colId++);
				cell.setCellValue(cc.getHeader());
				cell.setCellStyle(headerStyle);
			}
			
		} else if (data.isEmpty()) {
			if (data.getEmptyTableText()!=null && data.getEmptyTableText().trim().length()>0) {
				cell = row.createCell(colId++);
				cell.setCellValue(data.getEmptyTableText());
			}
		}
		
		boolean valueSet = false;
		
		// Add lines
		if (!data.isEmpty()) {

			GenericCell gc = null;
			for (GenericRow rr : data.getRows()) {
				colId = 0;
				row = sheet.createRow(rowId++);
				for (Object oc : rr.getContent()) {
					cell = row.createCell(colId++);
					if (oc instanceof GenericCell) {
						gc = (GenericCell)oc;
						valueSet = false;
						if (gc.getOriginalData()!=null) {
							
							valueSet = setCellValueFromOriginalData(gc, cell);
							
						}
						if (!valueSet && gc.getData()!=null) {
							cell.setCellValue(gc.getData().toString());
						}
					} else {
						if (oc!=null)
							cell.setCellValue(oc.toString());
					}
					
				}
				
			}
			
		}

		// Autosize columns
		for (colId = 0; colId < data.getColumns().size(); colId++) {
			sheet.autoSizeColumn(colId);
		}
		
		String path = getPath();
		
		if (!path.toLowerCase().endsWith(".xls")) {
			path += ".xls";
		}
		FileOutputStream out = new FileOutputStream(path);
		wb.write(out);
		out.close();
		((HSSFWorkbook)wb).close();

		return path;
		
	}

	private boolean setCellValueFromOriginalData(GenericCell gc, Cell cell) {
		
		Object od = gc.getOriginalData();
		if (od instanceof Number) {
			cell.setCellValue(((Number)od).doubleValue());
			return true;
		}
		if (od instanceof Date) {
			cell.setCellValue(((Date)od));
			return true;
		}

		return false;
	}
	
	@Override
	public GenericTable getClazz() {
		return new GenericTable();
	}

	@Override
	public String[] getFormats() {
		
		return formats;
		
	}

}
