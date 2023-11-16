package org.notima.businessobjects.adapter.fortnox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.InvoiceRow;
import org.notima.api.fortnox.entities3.InvoiceRows;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.InvoiceOperationResult;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.util.LocalDateUtils;

public class FortnoxInvoiceWriter {

	private FortnoxAdapter	adapter;
	private FortnoxClient3 fortnoxClient;
	private InvoiceOperationResult result = new InvoiceOperationResult();
	private LocalDate	invoiceDate;
	private LocalDate	dueDate;
	private boolean 	updateExisting = true;
	private boolean		createBusinessPartner = true;
	private int			createLimit;
	// Create an e-mail / bp number mapping
	private Map<String, String> custMap = new TreeMap<String,String>();
	private Map<String, BusinessPartner<?>> custMapById = new TreeMap<String, BusinessPartner<?>>();
	private Map<TaxSubjectIdentifier, BusinessPartner<?>> custMapByTaxId = new TreeMap<TaxSubjectIdentifier, BusinessPartner<?>>();
	
	
	public FortnoxInvoiceWriter(FortnoxAdapter adapter) {
		
		this.adapter = adapter;
		fortnoxClient = adapter.getClient();
		
	}
	
	private void refreshCustomerMaps() throws Exception {
		
		Location loc;
		List<BusinessPartner<?>> bpList = getAllBusinessPartners();
		
		for (BusinessPartner<?> bp : bpList) {
			if (bp.getAddressOfficial().getAddress1()==null) {
				System.out.println("Problem with " + bp.getName() + " : " + bp.getIdentityNo());
			}
			// Use delivery address for mapping if it exists
			loc = bp.getAddressShipping()!=null ? bp.getAddressShipping() : bp.getAddressOfficial();
			custMap.put(loc.getAddress1().trim().toUpperCase(), bp.getIdentityNo());
			custMapById.put(bp.getIdentityNo(), bp);
			custMapByTaxId.put(new TaxSubjectIdentifier(bp), bp);
			FortnoxAdapter.logger.info("Adding " + bp.getName() + " => " + bp.getIdentityNo());
		}
		
		
	}
	
	public InvoiceOperationResult writeInvoices(List<Invoice<?>> canonicalInvoices) throws Exception {
		
		initDates();
		
		// Set export revenue account
		// ((FortnoxBusinessObjectFactoryV3)fortnox3).setExportRevenueAccount("3105");
		adapter.setDefaultRevenueAccount("3011");
		
		refreshCustomerMaps();
		
		int ocount = 0;
		String custNo;
		BusinessPartner<?> bp;
		BusinessPartner<?> lookedUpBp;
		Location loc;
		// Make sure all business partners are imported
		for (Invoice<?> canonicalInvoice : canonicalInvoices) {
			custNo = null;
			lookedUpBp = null;
			
			bp = canonicalInvoice.getBusinessPartner();

			if (bp.getIdentityNo()!=null && bp.getIdentityNo().trim().length()>0) {
				lookedUpBp = custMapById.get(bp.getIdentityNo());
			}
			if (lookedUpBp==null)  {
				if (bp.hasLocations()) {
					loc = bp.getAddressShipping()!=null ? bp.getAddressShipping() : bp.getAddressOfficial();
					custNo = custMap.get(loc.getAddress1().trim().toUpperCase());
					if (custNo!=null) {
						bp.setIdentityNo(custNo);
					}
				} else if (bp.hasTaxId()) {
					lookedUpBp = custMapByTaxId.get(new TaxSubjectIdentifier(FortnoxUtil.convertTaxIdToFortnoxFormat(bp.getTaxId()),bp.getCountryCode()));
					if (lookedUpBp!=null) {
						bp.setIdentityNo(lookedUpBp.getIdentityNo());
					}
				} else {
					if (!createBusinessPartner) {
						throw new Exception("Can't find business partner " + custNo + " " + bp.getName());
					}
					// New customer
					bp.setIdentityNo(custNo);
					FortnoxAdapter.logger.info("Persisting new customer " + bp.getName());
					adapter.persist(bp);
				}
			}
			ocount++;
			
		}
		
		// Get existing invoices
		Map<Object,Object> invoiceMap = adapter.lookupList("InvoiceSubset-ExternalInvoiceReference2");
		
		ocount = 0;
		InvoiceSubset is = null;
		org.notima.api.fortnox.entities3.Invoice finvoice = null;
		
		Customer fcustomer = null;
		
		for (Invoice<?> o : canonicalInvoices) {
			
			is = (InvoiceSubset)invoiceMap.get(o.getOrderKey());
			fcustomer = fortnoxClient.getCustomerByCustNo(o.getBusinessPartner().getIdentityNo());
			
			if (is==null) {
				// Get customer as named in Fortnox
				bp = o.getBusinessPartner();
				bp.setName(fcustomer.getName());
				o.setInvoiceDate(LocalDateUtils.asDate(invoiceDate));
				o.setDueDate(LocalDateUtils.asDate(dueDate));
				FortnoxAdapter.logger.info("Persisting invoice for order " + o.getOrderKey());
				adapter.persist(o);
				result.incrementCreated();
				
				ocount++;
			} else {
				// Update existing
				 finvoice = adapter.getClient().getInvoice(is.getDocumentNumber());
				 if (updateInvoice(o, finvoice)) {
					 adapter.persistNativeInvoice(finvoice);
					 FortnoxAdapter.logger.info("Updated invoice " + finvoice.getDocumentNumber());
					 result.incrementUpdated();
				 }
			}
			if (createLimit>0 && ocount>=createLimit) {
				FortnoxAdapter.logger.info("Create limit " + createLimit + " reached.");
				break;
			}

		}	
		
		return result;
	}
	
	private void initDates() {
		
		if (invoiceDate==null) {
			invoiceDate = LocalDateUtils.asLocalDate(Calendar.getInstance().getTime());
		}
		
		if (dueDate==null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, 10);
			dueDate = LocalDateUtils.asLocalDate(cal.getTime());
		}
		
	}
	
	private List<BusinessPartner<?>> getAllBusinessPartners() throws Exception {
		
		// Get all customers
		List<BusinessPartner<Customer>> bpList = adapter.lookupAllBusinessPartners();
		FortnoxAdapter.logger.info(bpList.size() + " existing customers");
		List<BusinessPartner<?>> result = new ArrayList<BusinessPartner<?>>();
		for (BusinessPartner<Customer> bp : bpList) {
			bp = adapter.lookupBusinessPartner(bp.getIdentityNo());
			if (bp.getNativeBusinessPartner()!=null && 
					(bp.getNativeBusinessPartner().getCurrency()==null || bp.getNativeBusinessPartner().getCurrency().trim().length()==0)) {
				throw new Exception("No default currency set on customer " + bp.getNativeBusinessPartner().getCustomerNumber());
			}
			result.add(bp);
		}
		
		return result;
	}

	/**
	 * Only updates prices on existing invoices
	 * 
	 * @param src
	 * @param dst
	 * @return	true if anything was updated.
	 */
	private boolean updateInvoice(Invoice<?> src, org.notima.api.fortnox.entities3.Invoice dst) {

		boolean result = false;
		List<InvoiceLine> srcLines = src.getLines();
		InvoiceRows ir = dst.getInvoiceRows();
		List<InvoiceRow> dstLines = ir.getInvoiceRow();
		if (dstLines==null) {
			dstLines = new ArrayList<InvoiceRow>();
			ir.setInvoiceRow(dstLines);
		}
		
		InvoiceRow dstLine;
		for (InvoiceLine srcLine : srcLines) {
			
			dstLine = getCorrespondingRow(srcLine, dstLines);
			if (dstLine!=null && srcLine.getPriceActual()!=dstLine.getPrice()) {
				dstLine.setPrice(srcLine.getPriceActual());
				dstLine.setTotal(null); // Don't update total on line level
				result = true;
			}
			if (dstLine==null) {
				// Create a new line
				try {
					adapter.addCanonicalInvoiceLineToFortnoxInvoiceRow(src, srcLine, dstLines);
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
			
		}
		if (result) {
			dst.setTotal(null); // Don't update total
		}
		return result;
		
	}
	
	/**
	 * Returns corresponding row. Null if no corresponding row is found.
	 * 
	 * @param srcLine
	 * @return
	 */
	private InvoiceRow getCorrespondingRow(InvoiceLine srcLine, List<InvoiceRow> rows) {

		if (rows==null || srcLine==null)
			return null;
		InvoiceRow result = null;
		for (InvoiceRow r : rows) {
			if (srcLine.getProductKey()==null || srcLine.getDescription()==null) 
				continue;
			if (srcLine.getProductKey().equals(r.getArticleNumber()) && srcLine.getDescription().equals(r.getDescription())) {
				// Lets assume same product and description equals the same line
				result = r;
				break;
			}
		}
		return result;
	}
	
	public LocalDate getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(LocalDate invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public boolean isUpdateExisting() {
		return updateExisting;
	}

	public void setUpdateExisting(boolean updateExisting) {
		this.updateExisting = updateExisting;
	}

	public boolean isCreateBusinessPartner() {
		return createBusinessPartner;
	}

	public void setCreateBusinessPartner(boolean createBusinessPartner) {
		this.createBusinessPartner = createBusinessPartner;
	}

	public int getCreateLimit() {
		return createLimit;
	}

	public void setCreateLimit(int createLimit) {
		this.createLimit = createLimit;
	}

	public InvoiceOperationResult getResult() {
		return result;
	}

	public void setResult(InvoiceOperationResult result) {
		this.result = result;
	}

	
	
	
}
