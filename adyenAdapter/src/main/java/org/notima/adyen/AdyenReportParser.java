package org.notima.adyen;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.notima.generic.businessobjects.PayoutFee;
import org.notima.generic.ifacebusinessobjects.PaymentReportRow;
import org.notima.util.LocalDateUtils;

/**
 * Class that parses an Adyen settlement detail report batch.
 * 
 * 
 */
public class AdyenReportParser {

	private FormulaEvaluator evaluator;
	// Remember last workbook in case we're reading sheets
	private static Workbook		 lastWorkbook;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";    
	
	private String 			filename;
	private AdyenReport	report;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private List<AdyenReportRow> reportLines;
    private List<PaymentReportRow> payoutLines;
    private List<PayoutFee> feeLines;
    private List<PaymentReportRow> reportRows;
    private Map<String, List<AdyenReportRow>> pspReferenceMap;
    
    public static AdyenReport createFromFile(String filename) throws IOException, Exception {
    	AdyenReportParser parser = new AdyenReportParser(filename);
    	return parser.parseAdyenFile();
    }
    
    /**
     * Private constructor to hide the inner workings of this class.
     * 
     * @param filename
     */
    private AdyenReportParser(String filename) {
    	this.filename = filename;
    	initStructures();
    }
    
    private AdyenReport parseAdyenFile() throws IOException, Exception {
    	readRowsFromExcelFile(filename);
    	lastWorkbook.close();
    	report.setPayoutRows(payoutLines);
    	return report;
    }
    
    private void initStructures() {
    	report = new AdyenReport();
		reportLines = new ArrayList<AdyenReportRow>();
		payoutLines = new ArrayList<PaymentReportRow>();
		feeLines = new ArrayList<PayoutFee>();
		pspReferenceMap = new TreeMap<String, List<AdyenReportRow>>();
    	reportRows = new ArrayList<PaymentReportRow>();
    	report.setReportRows(reportRows);
    	report.setFeeRows(feeLines);
    }
    
    private void readRowsFromExcelFile (String path) throws IOException, Exception {

    	Workbook wb = WorkbookFactory.create(new File(path));
		lastWorkbook = wb;
		evaluator = wb.getCreationHelper().createFormulaEvaluator();
		evaluator.setIgnoreMissingWorkbooks(true);

		AdyenReportRow reportRow = null;
		
		// Get first sheet
		Sheet sheet = wb.getSheetAt(0);
		
		Row row;
		
		// Iterate through all rows, skip first row as that is title
		for (int r=1; r<sheet.getLastRowNum()+1; r++) {
			
			row = sheet.getRow(r);
			reportRow = parseRow(row);
			if (reportRow!=null) {
				addReportRow(reportRow);
			}
			
		}
		
    }

    private void addReportRow(AdyenReportRow reportRow) {
    	
    	reportLines.add(reportRow);
    	if (reportRow.isFee() || reportRow.isDepositAdjustment()) {
    		addFeeLine(reportRow);
    	}
    	if (reportRow.isPayout()) {
    		addPayoutLine(reportRow);
    	}
    	if (reportRow.hasPaymentReference()) {
    		addReportRowToReferenceMap(reportRow);
    	}
    	
    }

    private void addFeeLine(AdyenReportRow reportRow) {
    	PayoutFee payoutFee = new PayoutFee();
    	payoutFee.setDescription(reportRow.getModificationReference());
    	payoutFee.setCurrency(reportRow.getNetCurrency());
    	if (reportRow.isDepositAdjustment()) {
    		payoutFee.setDepositAdjustment(true);
    		payoutFee.setAmount(-reportRow.getAmount());
    	} else {
        	payoutFee.setFeeAmount(-reportRow.getAmount());
    	}
    	feeLines.add(payoutFee);
    }
    
    /**
     * Sets settlement date from payout
     * 
     * @param reportRow
     */
    private void addPayoutLine(AdyenReportRow reportRow) {
		if (reportRow.getCreationDate()!=null) {
			report.setSettlementDate(LocalDateUtils.asLocalDate(reportRow.getCreationDate()));
			report.setCurrency(reportRow.getNetCurrency());
			report.addToTotalAmount(reportRow.getAmount());
		}
    }
    
    private void addReportRowToReferenceMap(AdyenReportRow reportRow) {
    	reportRow.processFees();
    	reportRows.add(reportRow);
    	List<AdyenReportRow> rows = pspReferenceMap.get(reportRow.getPaymentReference());
    	if (rows==null) {
    		rows = new ArrayList<AdyenReportRow>();
    		pspReferenceMap.put(reportRow.getPaymentReference(), rows);
    	}
    	rows.add(reportRow);
    }
    
	private AdyenReportRow parseRow(Row row) throws ParseException {
		AdyenReportRow il = null;

		if (row==null) return null;
		
		Cell cell;
		il = new AdyenReportRow();
		
		for (int i=0; i<26; i++) {
			
			cell = row.getCell(i);
			
			if (cell==null) {
				continue;
			}
			
			switch(i) {
			
			
			case 0:	
				il.setCompanyAccount(getCellAsString(cell));
				break;
				
			case 1:
				il.setMerchantAccount(getCellAsString(cell));
				break;
				
			case 2:
				il.setPspReference(cell.getStringCellValue());
				break;
				
			case 3:
				il.setMerchantReference(getCellAsString(cell));
				break;
			
			case 4:
				il.setPaymentMethod(cell.getStringCellValue());
				break;
				
			case 5:
				il.setCreationDate(getDateFromCell(cell));
				break;
				
			case 6:
				il.setTimeZone(cell.getStringCellValue());
				break;
				
			case 7:
				il.setLineType(cell.getStringCellValue());
				break;
				
			case 8:
				il.setModificationReference(cell.getStringCellValue());
				break;
				
			case 9:
				il.setGrossCurrency(cell.getStringCellValue());
				break;
				
			case 10:
				il.setGrossDebit(getNumericFromCell(cell));
				break;
				
			case 11:
				il.setGrossCredit(getNumericFromCell(cell));
				break;
				
			case 12:
				il.setExchangeRate(getNumericFromCell(cell));
				break;
				
			case 13:
				il.setNetCurrency(cell.getStringCellValue());
				break;
				
			case 14:
				il.setNetDebit(getNumericFromCell(cell));
				break;
				
			case 15:
				il.setNetCredit(getNumericFromCell(cell));
				break;
				
			case 16:
				il.setCommission(getNumericFromCell(cell));
				break;
				
			case 17:
				il.setMarkup(getNumericFromCell(cell));
				break;
				
			case 18:
				il.setSchemeFees(getNumericFromCell(cell));
				break;
				
			case 19:
				il.setInterchange(getNumericFromCell(cell));
				break;
				
			case 20:
				il.setPaymentMethodVariant(cell.getStringCellValue());
				break;
				
			case 21:
				il.setModificationMerchantReference(cell.getStringCellValue());
				break;
				
			case 22:
				il.setBatchNumber(new Double(cell.getNumericCellValue()).intValue());
				break;
				
			case 23:
				il.setReserved4(cell.getStringCellValue());
				break;
					
			}
			
		}
		return il;
	}    
    
	private String getCellAsString(Cell c) {
	      if (c == null) {
	            return null;
	        }
	        if (c.getCellTypeEnum() == CellType.FORMULA) {
	            CellValue cellValue = evaluator.evaluate(c);
	            if (cellValue.getCellTypeEnum() == CellType.NUMERIC) {
	                return Integer.toString((int) cellValue.getNumberValue());
	            } else {
	                return cellValue.getStringValue();
	            }
	        }
	        return (c.getCellTypeEnum() == CellType.NUMERIC 
	                ? Integer.toString((int) c.getNumericCellValue()) : c.getStringCellValue());	}
	
	  /**
     * Retrieves the numeric value from a cell. The cell can either be a string representing a number
     * or an actual numeric cell. Returns 0 if the cell is empty.
     *
     * @param cell The cell to retrieve the numeric value from.
     * @return The numeric value of the cell as a Double, or 0 if the cell is empty.
     * @throws NumberFormatException if the cell contains a string that cannot be parsed as a number.
     */
    public Double getNumericFromCell(Cell cell) throws NumberFormatException {
        if (cell == null) {
            return 0.0;
        }

        if (cell.getCellTypeEnum() == CellType.STRING) {
            String numberString = cell.getStringCellValue();
            if (numberString.trim().length()==0)
            	return 0.0;
            try {
                return Double.parseDouble(numberString);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Cell contains a string that cannot be parsed as a number: " + numberString);
            }
        } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellTypeEnum() == CellType.BLANK) {
            return 0.0;
        } else {
            throw new IllegalArgumentException("Cell type is not supported for numeric extraction.");
        }
    }
    
    /**
     * Retrieves the date value from a cell. The cell can either be a string representing a date or an actual date cell.
     *
     * @param cell The cell to retrieve the date from.
     * @return The date value of the cell.
     * @throws ParseException if the cell contains a string that cannot be parsed as a date.
     */
    private Date getDateFromCell(Cell cell) throws ParseException {
        if (cell == null) {
            return null;
        }

        if (cell.getCellTypeEnum() == CellType.STRING) {
            String dateString = cell.getStringCellValue();
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            return sdf.parse(dateString);
        } else if (cell.getCellTypeEnum() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                throw new IllegalArgumentException("Cell contains a numeric value that is not a date.");
            }
        } else {
            throw new IllegalArgumentException("Cell type is not supported for date extraction.");
        }
    }    
}
