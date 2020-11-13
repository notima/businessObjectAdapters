package org.notima.businessobjects.adapter.excel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXB;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.businessobjects.Location;

/**
 * Code to convert one or many spreadsheets into invoices.
 * 
 * @author Daniel Tamm
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ExcelToInvoices {

	private FormulaEvaluator evaluator;
	// Remember last workbook in case we're reading sheets
	private static Workbook		 lastWorkbook;
	private static int		 lastSheetNo = -1;
	private static int		 lastRowNo = -1;
	private static int		 lastColNo = -1;
	
	/**
	 * Create an excel file from customer list
	 * 
	 * @param  	bpList	A list of business partners.
	 * @param   path	Location to Excel file.	
	 */
	public void createExcelBpList(List<BusinessPartner> bpList, String path) throws Exception {
		
		Workbook wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet();
		
		Row row;
		int rowId = 0;
		
		for (BusinessPartner bp : bpList) {
			
			row = sheet.createRow(rowId++);
			fillRow(bp, row);
			
		}

		if (!path.toLowerCase().endsWith(".xls")) {
			path += path + ".xls";
		}
		FileOutputStream out = new FileOutputStream(path);
		wb.write(out);
		out.close();
		((HSSFWorkbook)wb).close();
		
	}

	/**
	 * Creates a spreadsheet row from a business partner.
	 * 
	 * @param bp			The business partner
	 * @param row			The row to get the business partner information
	 */
	public void fillRow(BusinessPartner bp, Row row) {
		
		int c = 0;
		
		Cell cell;
		// Kundnr
		cell = row.createCell(c++);
		cell.setCellValue(bp.getIdentityNo());
		// Last name
		cell = row.createCell(c++);
		cell.setCellValue(bp.getName());
		// First name
		cell = row.createCell(c++);
		cell.setCellValue(bp.getName());
		// Art nr
		cell = row.createCell(c++);
		// Text
		cell = row.createCell(c++);
		// Amount
		cell = row.createCell(c++);
		// Address
		Location loc = bp.getAddressOfficial();
		cell = row.createCell(c++);
		if (loc!=null)
			cell.setCellValue(loc.getAddress1());
		// Housenum
		cell = row.createCell(c++);
		// Zip
		cell = row.createCell(c++);
		if (loc!=null)
			cell.setCellValue(loc.getPostal());
		// City
		cell = row.createCell(c++);
		if (loc!=null)
			cell.setCellValue(loc.getCity());
		// Order
		cell = row.createCell(c++);
		
	}
	
	/**
	 * Parse an excel file with the given format
	 * 
	 * RealEstateNR; LastName; FirstName; ProductKey; InvoiceText; Amount; Address; HouseNo; Zip; City; OrderKey
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public List<Invoice<?>> parseExcelList(String path) throws Exception {
		
		Workbook wb = WorkbookFactory.create(new File(path));
		lastWorkbook = wb;
		evaluator = wb.getCreationHelper().createFormulaEvaluator();
		evaluator.setIgnoreMissingWorkbooks(true);
		
		List<Invoice<?>> result = new ArrayList<Invoice<?>>();
		Invoice<?> invoice = null;
		
		// Get first sheet
		Sheet sheet = wb.getSheetAt(0);
		lastSheetNo = 0;
		
		Row row;
		
		// Iterate through all rows, skip first row as that is title
		for (int r=1; r<sheet.getLastRowNum()+1; r++) {
			
			row = sheet.getRow(r);
			invoice = parseRow(row);
			if (invoice!=null) {
				result.add(invoice);
			}
			lastRowNo = r;
			
		}
		
		return result;
		
	}

	private Invoice parseRow(Row row) {
		InvoiceLine il = null;
		BusinessPartner bp = null;
		Location loc = null;

		if (row==null) return null;
		
		Invoice invoice = new Invoice();
		Cell cell;
		bp = new BusinessPartner();
		il = new InvoiceLine();
		loc = new Location();
		String accountNo;
		
		for (int i=0; i<12; i++) {
			
			cell = row.getCell(i);
			lastColNo = i;
			
			if (cell==null) {
				return null;
			}
			
			switch(i) {
			
			
			case 0:	
				bp.setIdentityNo(getCellAsString(cell));
				break;
				
			case 1:
				bp.setName(cell.getStringCellValue());
				break;
				
			case 2:
				bp.setName(bp.getName() +" " + cell.getStringCellValue());
				break;
				
			case 3:
				il.setProductKey(getCellAsString(cell));
				break;
			
			case 4:
				il.setDescription(cell.getStringCellValue());
				break;
				
			case 5:
				il.setQtyEntered(1);
				il.setPriceActual(Double.parseDouble(Long.toString(Math.round(cell.getNumericCellValue()))));
				break;
				
			case 6:
				loc.setAddress1(cell.getStringCellValue());
				break;
				
			case 7:
				loc.setAddress1(loc.getAddress1() + " " + getCellAsString(cell));
				break;
				
			case 8:
				loc.setPostal(getCellAsString(cell));
				break;
				
			case 9:
				loc.setCity(cell.getStringCellValue());
				break;
				
			case 10:
				invoice.setOrderKey(getCellAsString(cell));
				break;
				
			case 11:
				if (cell!=null) {
					accountNo = getCellAsString(cell);
					if (accountNo!=null && accountNo.trim().length()>0) {
						il.setAccountNo(accountNo);
					}
				}
				
			}
			
		}
		
		// Connect bp, location and invoice line
		// Check for multiple lines (separated by | )
		String[] lines = null;
		boolean emptyInvoiceLine = false;
		if (il.getDescription()!=null && il.getDescription().trim().length()>0) {
			lines = il.getDescription().split("\\|");
		}
		if ((il.getDescription()==null || il.getDescription().trim().length()==0) &&
				il.getPriceActual()==0) {
			emptyInvoiceLine = true;
		}
		if (!emptyInvoiceLine) {
			invoice.addInvoiceLine(il);
			if (lines!=null && lines.length>1) {
				il.setDescription(lines[0]);
				for (int i=1; i<lines.length; i++) {
					il = new InvoiceLine();
					il.setDescription(lines[i]);
					invoice.addInvoiceLine(il);
				}
			}
		}
		bp.setAddressOfficial(loc);
		invoice.setBusinessPartner(bp);
		
		return invoice;
	}
	

	/**
	 * RealEstateNR; ProductKey; InvoiceText; Qty; Price/Each 
	 * 
	 * @param invoiceMap		A map of invoices that additional billing can be applied to
	 * @param pathOrSheetNo				A file path (or the sheet number of the last used path / spreadsheet)
	 */
	public void parseAdditionalBilling(Map<String,Invoice> invoiceMap, String pathOrSheetNo) throws Exception {
		
		// Check if path is file name or a number (sheet number)
		Integer sheetNo = -1;
		try {
			sheetNo = Integer.parseInt(pathOrSheetNo);
		} catch (Exception pe) {
			sheetNo = -1;
		}
		
		Workbook wb = null;
		Sheet sheet = null;
		if (sheetNo<0) {
			wb = WorkbookFactory.create(new File(pathOrSheetNo));
			evaluator = wb.getCreationHelper().createFormulaEvaluator();
			// Get first sheet
			sheet = wb.getSheetAt(0);
			lastSheetNo = 0;
		} else {
			wb = lastWorkbook;
			sheet = wb.getSheetAt(sheetNo);
			lastSheetNo = sheetNo;
		}
		
		Row row;
		InvoiceLine il = new InvoiceLine();
		
		// Iterate through all rows, skip first row as that is title
		for (int r=1; il!=null && r<sheet.getLastRowNum()+1; r++) {
			
			row = sheet.getRow(r);
			lastRowNo = r;
			il = parseInvoiceLine(invoiceMap,row);
			
		}
		
	}
	
	/**
	 * RealEstateNR; ProductKey; InvoiceText; Qty; Price/Each 
	 * 
	 * 
	 * @param invoiceMap		The invoice map to work on
	 * @param row				The row to be parsed.
	 * @return		An invoice line
	 */
	private InvoiceLine parseInvoiceLine(Map<String,Invoice> invoiceMap, Row row) throws Exception {

		if (row==null)
			return null;
		
		InvoiceLine il = new InvoiceLine();
		Invoice invoice = null;
		Cell cell;
		String bpId = null;
		String accountNo;
		String[] descriptionLines = null;
			
		for (int i=0; i<7; i++) {
			
			cell = row.getCell(i);
			lastColNo = i;
			
			switch(i) {
			
			case 0:
				bpId = getCellAsString(cell);
				// Lookup invoice
				if (bpId==null) return null;
				invoice = invoiceMap.get(bpId);
				if (invoice==null) return null;
				break;
				
			case 1:
				il.setProductKey(getCellAsString(cell));
				break;
			
			case 2:
				descriptionLines = getDescriptionLines(getCellAsString(cell));
				if (descriptionLines!=null && descriptionLines.length>0)
					il.setDescription(descriptionLines[0]);
				break;

			case 3:
				il.setQtyEntered(cell.getNumericCellValue());
				break;
				
			case 4:
				il.setPriceActual(Math.round(cell.getNumericCellValue()*100.0)/100.0);
				break;
				
			case 5:
				il.setUOM(getCellAsString(cell));
				break;
				
			case 6:
				if (cell!=null) {
					accountNo = getCellAsString(cell);
					if (accountNo!=null && accountNo.trim().length()>0) {
						il.setAccountNo(accountNo);
					}
				}
				
			}
			
		}

		invoice.addInvoiceLine(il);
		if (descriptionLines!=null && descriptionLines.length>1) {
			for (int i=1; i<descriptionLines.length; i++) {
				il = new InvoiceLine();
				il.setDescription(descriptionLines[i]);
				invoice.addInvoiceLine(il);
			}
		}
		
		return il;
		
		
	}
	
	
	private String getCellAsString(Cell c) {
		if (c==null) return null;
		if (c.getCellType()==Cell.CELL_TYPE_FORMULA) {
			CellValue cellValue = evaluator.evaluate(c);
			if (cellValue.getCellType()==Cell.CELL_TYPE_NUMERIC) {
				return Integer.toString((int)cellValue.getNumberValue());
			} else {
				return cellValue.getStringValue();
			}
		}
		return (c.getCellType()==Cell.CELL_TYPE_NUMERIC 
				? Integer.toString((int)c.getNumericCellValue()) : c.getStringCellValue());
	}
	
	
	/**
	 * If the description line contains | it's separated into lines.
	 * 
	 * @param description
	 * @return
	 */
	public String[] getDescriptionLines(String description) {
		
		if (description==null) return null;
		if (!description.contains("|")) {
			return new String[]{description};
		}
		
		String[] lines = description.split("\\|");
		return lines;
		
	}
	
	/**
	 * Creates an invoice list from an excel file
	 * 
	 * @param fileName		The excel file to parse
	 * @param additional	The additional sheets (or files) to include sheet 0 (first sheet, is always included), so start from 1.
	 * @return				A list of invoices
	 */
	public InvoiceList createInvoiceListFromFile(String fileName, List<String> additional) throws Exception {
		

		ExcelToInvoices billing = new ExcelToInvoices();
		
		// Try to find file
		File f = new File(fileName);
		if (!f.exists()) {
			// Try to look in classpath
			URL url = billing.getClass().getResource(fileName);
			if (url!=null) {
				fileName = url.getFile();
			}
		}
		
		List<Invoice<?>> invoices = billing.parseExcelList(fileName);

		if (additional.size()>1) {
			// See if additional billing lines should be added
			Map<String,Invoice> invoiceMap = new TreeMap<String,Invoice>();
			
			for (Invoice invoice : invoices) {
				invoiceMap.put(invoice.getBusinessPartner().getIdentityNo(), invoice);
			}
			
			for (String sn : additional) {
			
				billing.parseAdditionalBilling(invoiceMap, sn);
				
			}
			
		}
		
		InvoiceList invoiceList = new InvoiceList();
		invoiceList.setInvoiceList(invoices);
		
		if (lastWorkbook!=null) {
			lastWorkbook.close();
		}

		return invoiceList;
	}
	
	public static void main(String[] args) {

		try {
			if (args.length>0) {
				ExcelToInvoices billing = new ExcelToInvoices();
				
				// Try to find file
				String fileName = null;
				File f = new File(args[0]);
				if (!f.exists()) {
					// Try to look in classpath
					URL url = billing.getClass().getResource(args[0]);
					if (url!=null) {
						fileName = url.getFile();
					}
				}
				if (fileName==null)
					fileName = args[0];
				
				List<Invoice<?>> invoices = billing.parseExcelList(fileName);
				// Get path from file
				File file = new File(fileName);
				File path = file.getParentFile();

				if (args.length>1) {
					// See if additional billing lines should be added
					Map<String,Invoice> invoiceMap = new TreeMap<String,Invoice>();
					
					for (Invoice invoice : invoices) {
						invoiceMap.put(invoice.getBusinessPartner().getIdentityNo(), invoice);
					}
					
					for (int i=1; i<args.length; i++) {
					
						billing.parseAdditionalBilling(invoiceMap, args[i]);
						
					}
					
				}
				
				InvoiceList invoiceList = new InvoiceList();
				invoiceList.setInvoiceList(invoices);
				
				File outFile = new File(path.getAbsolutePath() + File.separator + "out.xml");
				FileWriter fileWriter = new FileWriter(outFile);
				
				JAXB.marshal(invoiceList, fileWriter);
				fileWriter.close();
				
				System.out.println("Wrote xml to file " + outFile.getAbsolutePath());
				
				if (lastWorkbook!=null) {
					lastWorkbook.close();
				}
				
			}
		} catch (Exception e) {
			System.err.println("Exception at sheet# : " + lastSheetNo + " Row# : " + lastRowNo + " Col# : " + lastColNo);
			e.printStackTrace();
			if (lastWorkbook!=null) {
				try {
					lastWorkbook.close();
				} catch (Exception eee) {
					
				}
			}
		}
		
	}

}
