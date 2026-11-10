package org.notima.businessobjects.adapter.fortnox;

import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.OrderSubset;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceWriterOptions;
import org.notima.generic.businessobjects.OrderList;
import org.notima.generic.businessobjects.Person;
import org.notima.util.LocalDateUtils;

/**
 * Writes canonical (sales) orders to Fortnox.
 *
 * <p>The order key of each order (its reference in the source system, e.g. a web shop's
 * order id) is written to ExternalInvoiceReference1. An order whose order key is already
 * found on an order in Fortnox is skipped, so the same orders can be written again without
 * creating duplicates.
 *
 * <p>The customer of each order is looked up in this order:
 * <ol>
 *   <li>Customer number (identityNo of the business partner) if set.</li>
 *   <li>Organisation number (taxId) for companies.</li>
 *   <li>E-mail address.</li>
 * </ol>
 * If no customer is found, a new customer is created if
 * {@link OrderInvoiceWriterOptions#isCreateBusinessPartner()} is set, otherwise the order
 * is not written.
 */
public class FortnoxOrderWriter extends FortnoxObjectWriter {

	private OrderInvoiceOperationResult result = new OrderInvoiceOperationResult();

	public FortnoxOrderWriter(FortnoxAdapter adapter, OrderInvoiceWriterOptions opts) {

		this.adapter = adapter;
		this.options = opts!=null ? opts : new OrderInvoiceWriterOptions();
		fortnoxClient = adapter.getClient();

	}

	/**
	 * Writes the orders to Fortnox.
	 *
	 * @param canonicalOrders	The orders to write.
	 * <p>An order that can't be written is logged and the remaining orders are written.
	 * Orders not written have no document key afterwards; orders written now or before have
	 * the Fortnox order number as document key.
	 *
	 * @return					The result, not successful if any order failed. Created orders
	 * 							are found in affected orders.
	 * @throws Exception		If something goes wrong.
	 */
	public OrderInvoiceOperationResult writeOrders(List<Order<?>> canonicalOrders) throws Exception {

		OrderList affected = new OrderList();
		result.setAffectedOrders(affected);

		int ocount = 0;
		int failed = 0;

		for (Order<?> o : canonicalOrders) {

			if (options.getCreateLimit()>0 && ocount>=options.getCreateLimit()) {
				FortnoxAdapter.logger.info("Create limit " + options.getCreateLimit() + " reached.");
				break;
			}

			try {
				if (writeOrder(o)) {
					result.incrementCreated();
					affected.getOrderList().add(o);
					ocount++;
				}
			} catch (Exception e) {
				failed++;
				FortnoxAdapter.logger.error("Order " + o.getOrderKey() + " could not be written: " + e.getMessage(), e);
			}

		}

		result.setSuccessful(failed==0);
		return result;
	}

	/**
	 * Writes one order.
	 *
	 * @param o				The order.
	 * @return				True if the order was created. False if it already exists or
	 * 						its customer is not found (and may not be created).
	 * @throws Exception	If the order can't be written.
	 */
	private boolean writeOrder(Order<?> o) throws Exception {

		List<OrderSubset> existing = fortnoxClient.getOrdersByExternalInvoiceReference1(o.getOrderKey());
		if (!existing.isEmpty()) {
			o.setDocumentKey(existing.get(0).getDocumentNumber());
			FortnoxAdapter.logger.info("Order " + o.getOrderKey() + " already exists as Fortnox order "
					+ existing.get(0).getDocumentNumber() + ", skipping.");
			return false;
		}

		BusinessPartner<?> bp = o.getBusinessPartner();
		Customer customer = lookupCustomer(bp);
		if (customer==null) {
			if (!options.isCreateBusinessPartner()) {
				FortnoxAdapter.logger.warn("Customer " + (bp!=null ? bp.getName() : "")
						+ " of order " + o.getOrderKey() + " not found in Fortnox, order not written.");
				return false;
			}
			FortnoxAdapter.logger.info("Creating new customer " + bp.getName());
			bp.setIdentityNo(null);
			adapter.persist(bp);
		} else {
			bp.setIdentityNo(customer.getCustomerNumber());
		}

		if (o.getDateOrdered()==null && options.getOrderDate()!=null) {
			o.setDateOrdered(LocalDateUtils.asDate(options.getOrderDate()));
		}

		adapter.persist(o);
		FortnoxAdapter.logger.info("Order " + o.getOrderKey() + " written as Fortnox order " + o.getDocumentKey());
		return true;
	}

	/**
	 * Looks up the Fortnox customer of the business partner.
	 *
	 * @param bp			The business partner.
	 * @return				The customer, null if not found.
	 * @throws Exception	If something goes wrong.
	 */
	protected Customer lookupCustomer(BusinessPartner<?> bp) throws Exception {

		if (bp==null)
			throw new Exception("Order without business partner");

		Customer c = null;

		if (bp.getIdentityNo()!=null && bp.getIdentityNo().trim().length()>0) {
			c = fortnoxClient.getCustomerByCustNo(bp.getIdentityNo().trim());
			if (c!=null) return c;
		}

		if (bp.isCompany() && bp.hasTaxId()) {
			try {
				c = fortnoxClient.getCustomerByTaxId(bp.getTaxId(), true);
				if (c!=null) return c;
			} catch (Exception e) {
				// Not found
			}
		}

		String email = getEmail(bp);
		if (email!=null) {
			c = fortnoxClient.getCustomerByEmail(email);
		}

		return c;
	}

	private String getEmail(BusinessPartner<?> bp) {
		Location loc = bp.getAddressOfficial();
		if (loc!=null && loc.getEmail()!=null && loc.getEmail().trim().length()>0) {
			return loc.getEmail().trim();
		}
		if (bp.getContacts()!=null) {
			for (Person p : bp.getContacts()) {
				if (p.getEmail()!=null && p.getEmail().trim().length()>0) {
					return p.getEmail().trim();
				}
			}
		}
		return null;
	}

	public OrderInvoiceOperationResult getResult() {
		return result;
	}

	public FortnoxClient3 getFortnoxClient() {
		return fortnoxClient;
	}

}
