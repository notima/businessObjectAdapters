package org.notima.businessobjects.adapter.excel;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.ReportFormatter;
import org.notima.businessobjects.adapter.tools.table.GenericCell;
import org.notima.businessobjects.adapter.tools.table.GenericColumn;
import org.notima.businessobjects.adapter.tools.table.GenericRow;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

/**
 * Writes a generic table to an Excel file.
 *
 * Cells are aligned as their column. Numbers are written as numbers: decimal numbers
 * (amounts) with two decimals and thousand separator, integers without decimals.
 * Dates are written as dates. A cell's original data, if set, is used rather than its
 * formatted text. The column widths are adapted to the content.
 */
public class ExcelReportFormatter extends BasicReportFormatter implements ReportFormatter<GenericTable> {

	public static final String[] formats = new String[] {
		"xlsx",
		"xls",
		"ods"
	};

	public static final String AMOUNT_FORMAT = "#,##0.00";
	public static final String INTEGER_FORMAT = "0";
	public static final String DATE_FORMAT = "yyyy-mm-dd";

	/** Extra width (1/256 of a character) added to auto sized columns, since bold headers are cut otherwise. */
	private static final int COLUMN_PADDING = 512;
	/** Maximum column width in characters. */
	private static final int MAX_COLUMN_CHARS = 80;

	private Workbook wb;
	private List<ColumnStyles> columnStyles;
	private int[] maxTextLength;

	@Override
	public synchronized String formatReport(GenericTable data, String format, Properties props) throws Exception {

		setFromProperties(props);

		wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet();
		createColumnStyles(data);
		maxTextLength = new int[data.getColumns().size()];

		Row row;
		Cell cell;
		int rowId = 0;
		int colId = 0;

		// Create first row
		row = sheet.createRow(rowId++);

		if (data.hasHeaders()) {

			for (GenericColumn cc : data.getColumns()) {
				cell = row.createCell(colId);
				cell.setCellValue(cc.getHeader());
				cell.setCellStyle(columnStyles.get(colId).header);
				registerLength(colId, cc.getHeader());
				colId++;
			}

		} else if (data.isEmpty()) {
			if (data.getEmptyTableText()!=null && data.getEmptyTableText().trim().length()>0) {
				cell = row.createCell(colId++);
				cell.setCellValue(data.getEmptyTableText());
			}
		}

		// Add lines
		if (!data.isEmpty()) {

			for (GenericRow rr : data.getRows()) {
				colId = 0;
				row = sheet.createRow(rowId++);
				for (Object oc : rr.getContent()) {
					cell = row.createCell(colId);
					setCellValue(cell, colId, oc);
					colId++;
				}
			}

		}

		autoSizeColumns(sheet, data.getColumns().size());

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

	/**
	 * Sets the value of a cell. Numbers and dates are written as such, using the cell's
	 * original data if it's a generic cell.
	 */
	private void setCellValue(Cell cell, int colId, Object oc) {

		ColumnStyles styles = colId < columnStyles.size() ? columnStyles.get(colId) : null;
		Object value = oc;
		if (oc instanceof GenericCell) {
			GenericCell gc = (GenericCell)oc;
			Object od = gc.getOriginalData();
			value = (od instanceof Number || od instanceof Date) ? od : gc.getData();
		}
		if (value==null) {
			if (styles!=null) cell.setCellStyle(styles.text);
			return;
		}

		if (value instanceof Number) {
			Number n = (Number)value;
			cell.setCellValue(n.doubleValue());
			boolean integer = isInteger(n);
			if (styles!=null) cell.setCellStyle(integer ? styles.integer : styles.amount);
			// The formatted text is what's shown, use it for the column width.
			registerLength(colId, integer ? String.valueOf(n.longValue()) : String.format("%,.2f", n.doubleValue()));
			return;
		}
		if (value instanceof Date) {
			cell.setCellValue((Date)value);
			if (styles!=null) cell.setCellStyle(styles.date);
			registerLength(colId, DATE_FORMAT);
			return;
		}

		String text = value.toString();
		cell.setCellValue(text);
		if (styles!=null) cell.setCellStyle(styles.text);
		registerLength(colId, text);

	}

	private boolean isInteger(Number n) {
		return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte || n instanceof BigInteger
				|| (n instanceof BigDecimal && ((BigDecimal)n).scale()<=0);
	}

	/**
	 * Creates the cell styles of each column, aligned as the column.
	 */
	private void createColumnStyles(GenericTable data) {

		DataFormat df = wb.createDataFormat();
		Font headerFont = wb.createFont();
		headerFont.setBold(true);

		columnStyles = new ArrayList<ColumnStyles>();
		for (GenericColumn cc : data.getColumns()) {
			HorizontalAlignment alignment = toAlignment(cc.getAlignment());
			ColumnStyles cs = new ColumnStyles();
			cs.header = createStyle(alignment, null);
			cs.header.setFont(headerFont);
			cs.text = createStyle(alignment, null);
			// Numbers are right aligned unless the column says otherwise.
			HorizontalAlignment numberAlignment = GenericColumn.ALIGNMENT_LEFT.equalsIgnoreCase(cc.getAlignment()) ? HorizontalAlignment.RIGHT : alignment;
			cs.amount = createStyle(numberAlignment, df.getFormat(AMOUNT_FORMAT));
			cs.integer = createStyle(numberAlignment, df.getFormat(INTEGER_FORMAT));
			cs.date = createStyle(alignment, df.getFormat(DATE_FORMAT));
			columnStyles.add(cs);
		}

	}

	private CellStyle createStyle(HorizontalAlignment alignment, Short dataFormat) {
		CellStyle style = wb.createCellStyle();
		style.setAlignment(alignment);
		if (dataFormat!=null)
			style.setDataFormat(dataFormat);
		return style;
	}

	private HorizontalAlignment toAlignment(String alignment) {
		if (GenericColumn.ALIGNMENT_RIGHT.equalsIgnoreCase(alignment))
			return HorizontalAlignment.RIGHT;
		if (GenericColumn.ALIGNMENT_CENTER.equalsIgnoreCase(alignment))
			return HorizontalAlignment.CENTER;
		return HorizontalAlignment.LEFT;
	}

	private void registerLength(int colId, String text) {
		if (text!=null && colId < maxTextLength.length && text.length() > maxTextLength[colId])
			maxTextLength[colId] = text.length();
	}

	/**
	 * Adapts the column widths to the content. Auto sizing needs the fonts of the system,
	 * if that fails (e.g. a headless server without fonts) the width is estimated from the
	 * length of the text.
	 */
	private void autoSizeColumns(Sheet sheet, int columns) {
		for (int colId = 0; colId < columns; colId++) {
			int estimated = Math.min(maxTextLength[colId] + 2, MAX_COLUMN_CHARS) * 256;
			int width;
			try {
				sheet.autoSizeColumn(colId);
				width = sheet.getColumnWidth(colId) + COLUMN_PADDING;
				// Without fonts the auto size can be (close to) zero.
				if (width <= COLUMN_PADDING + 256 && maxTextLength[colId] > 1)
					width = estimated;
			} catch (Throwable t) {
				width = estimated;
			}
			sheet.setColumnWidth(colId, Math.min(width, MAX_COLUMN_CHARS * 256));
		}
	}

	@Override
	public GenericTable getClazz() {
		return new GenericTable();
	}

	@Override
	public String[] getFormats() {

		return formats;

	}

	/**
	 * The cell styles of a column.
	 */
	private static class ColumnStyles {
		CellStyle header;
		CellStyle text;
		CellStyle amount;
		CellStyle integer;
		CellStyle date;
	}

}
