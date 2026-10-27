package org.notima.businessobjects.adapter.excel.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Test;
import org.notima.businessobjects.adapter.excel.ExcelReportFormatter;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.table.GenericCell;
import org.notima.businessobjects.adapter.tools.table.GenericColumn;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class TestExcelReportFormatter {

	@Test
	public void testAmountsAlignmentAndWidth() throws Exception {

		GenericTable table = new GenericTable();
		table.addColumn("#", GenericColumn.ALIGNMENT_RIGHT);
		table.addColumn("Name");
		table.addColumn("Amount", GenericColumn.ALIGNMENT_RIGHT);
		GenericCell amount = new GenericCell("1 234 567,50");
		amount.setOriginalData(1234567.5d);
		table.addRow().addContent(1, "A rather long customer name AB", amount);

		File out = File.createTempFile("excelreport", ".xls");
		out.deleteOnExit();
		Properties props = new Properties();
		props.setProperty(BasicReportFormatter.OUTPUT_FILENAME, out.getAbsolutePath());
		String path = new ExcelReportFormatter().formatReport(table, "xls", props);

		FileInputStream in = new FileInputStream(path);
		HSSFWorkbook wb = new HSSFWorkbook(in);
		in.close();
		Sheet sheet = wb.getSheetAt(0);

		Cell count = sheet.getRow(1).getCell(0);
		assertEquals(CellType.NUMERIC, count.getCellTypeEnum());
		assertEquals(ExcelReportFormatter.INTEGER_FORMAT, count.getCellStyle().getDataFormatString());

		Cell amt = sheet.getRow(1).getCell(2);
		assertEquals(CellType.NUMERIC, amt.getCellTypeEnum());
		assertEquals(1234567.5d, amt.getNumericCellValue(), 0.001);
		assertEquals(ExcelReportFormatter.AMOUNT_FORMAT, amt.getCellStyle().getDataFormatString());
		assertEquals(HorizontalAlignment.RIGHT, amt.getCellStyle().getAlignmentEnum());
		assertEquals(HorizontalAlignment.RIGHT, sheet.getRow(0).getCell(2).getCellStyle().getAlignmentEnum());

		Cell name = sheet.getRow(1).getCell(1);
		assertEquals(CellType.STRING, name.getCellTypeEnum());
		assertEquals(HorizontalAlignment.LEFT, name.getCellStyle().getAlignmentEnum());

		// The name column is wider than the short count column.
		assertTrue(sheet.getColumnWidth(1) > sheet.getColumnWidth(0));
		assertTrue(sheet.getColumnWidth(1) >= 20 * 256);

		wb.close();
	}

}
