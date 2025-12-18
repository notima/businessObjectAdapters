package org.notima.businessobjects.adapter.fortnox;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.api.fortnox.entities3.OrderRow;
import org.notima.api.fortnox.entities3.OrderRows;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceWriterOptions;
import org.notima.generic.businessobjects.OrderLine;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.util.LocalDateUtils;

public class FortnoxOrderWriter extends FortnoxObjectWriter {

	private OrderInvoiceOperationResult result = new OrderInvoiceOperationResult();
	
	private OrderInvoiceWriterOptions	options;
	
	
	
	public FortnoxOrderWriter(FortnoxAdapter adapter, OrderInvoiceWriterOptions opts) {
		
		this.adapter = adapter;
		options = opts;
		fortnoxClient = adapter.getClient();
		
	}
	
	
	public OrderInvoiceOperationResult writeOrders(List<Order<?>> canonicalOrders) throws Exception {
		
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
		for (Order<?> canonicalInvoice : canonicalOrders) {
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
					if (!options.isCreateBusinessPartner()) {
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
		Map<Object,Object> invoiceMap = adapter.lookupList("InvoiceSubset-ExternalInvoiceReference2", true);
		
		ocount = 0;
		InvoiceSubset is = null;
		org.notima.api.fortnox.entities3.Order finvoice = null;
		
		Customer fcustomer = null;
		
		for (Order<?> o : canonicalOrders) {
			
			is = (InvoiceSubset)invoiceMap.get(o.getOrderKey());
			fcustomer = getFortnoxCustomer(o);
			
			if (is==null) {
				// Get customer as named in Fortnox
				bp = o.getBusinessPartner();
				bp.setName(fcustomer.getName());
				bp.setIdentityNo(fcustomer.getCustomerNumber());
				o.setDateOrdered(LocalDateUtils.asDate(options.getInvoiceDate()));
				FortnoxAdapter.logger.info("Persisting order " + o.getOrderKey());
				adapter.persist(o);
				result.incrementCreated();
				
				ocount++;
			} else {
				// Update existing
				 finvoice = adapter.getClient().getOrder(is.getDocumentNumber());
				 if (updateOrder(o, finvoice)) {
					 adapter.persistNativeOrder(finvoice);
					 FortnoxAdapter.logger.info("Updated order " + finvoice.getDocumentNumber());
					 result.incrementUpdated();
				 }
			}
			if (options.getCreateLimit()>0 && ocount>=options.getCreateLimit()) {
				FortnoxAdapter.logger.info("Create limit " + options.getCreateLimit() + " reached.");
				break;
			}

		}	
		
		return result;
	}
	
	private void initDates() {
		
		if (options.getOrderDate()==null) {
			options.setOrderDate(LocalDateUtils.asLocalDate(Calendar.getInstance().getTime()));
		}
		
		if (options.getDueDate()==null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, 10);
			options.setDueDate(LocalDateUtils.asLocalDate(cal.getTime()));
		}
		
	}

	/**
	 * Only updates prices on existing invoices
	 * 
	 * @param src
	 * @param dst
	 * @return	true if anything was updated.
	 */
	private boolean updateOrder(Order<?> src, org.notima.api.fortnox.entities3.Order dst) {

		boolean result = false;
		List<OrderLine> srcLines = src.getLines();
		OrderRows ir = dst.getOrderRows();
		List<OrderRow> dstLines = ir.getOrderRow();
		if (dstLines==null) {
			dstLines = new ArrayList<OrderRow>();
			ir.setOrderRow(dstLines);
		}
		
		OrderRow dstLine;
		for (OrderLine srcLine : srcLines) {
			
			dstLine = getCorrespondingRow(srcLine, dstLines);
			if (dstLine!=null && srcLine.getPriceActual()!=dstLine.getPrice()) {
				dstLine.setPrice(srcLine.getPriceActual());
				dstLine.setTotal(null); // Don't update total on line level
				result = true;
			}
			if (dstLine==null) {
				// Create a new line
				try {
					adapter.addCanonicalOrderLineToFortnoxOrderRow(src, srcLine, dstLines);
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
			
		}
		if (result) {
			dst.setTotal(0D); // Don't update total
		}
		return result;
		
	}
	
	/**
	 * Returns corresponding row. Null if no corresponding row is found.
	 * 
	 * @param srcLine
	 * @return
	 */
	private OrderRow getCorrespondingRow(OrderLine srcLine, List<OrderRow> rows) {

		if (rows==null || srcLine==null)
			return null;
		OrderRow result = null;
		for (OrderRow r : rows) {
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

	public OrderInvoiceOperationResult getResult() {
		return result;
	}

	public void setResult(OrderInvoiceOperationResult result) {
		this.result = result;
	}

	
	
	
}
