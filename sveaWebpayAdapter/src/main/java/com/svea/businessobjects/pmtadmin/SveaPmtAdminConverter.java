package com.svea.businessobjects.pmtadmin;

import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;

import org.notima.api.webpay.pmtapi.PmtApiUtil;
import org.notima.api.webpay.pmtapi.entity.Credit;
import org.notima.api.webpay.pmtapi.entity.Delivery;
import org.notima.api.webpay.pmtapi.entity.OrderRow;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderLine;
import org.notima.generic.businessobjects.Person;

import com.svea.webpay.common.conv.InvalidTaxIdFormatException;
import com.svea.webpay.common.conv.TaxIdFormatter;
import com.svea.webpay.common.conv.TaxIdStructure;
import com.svea.webpay.common.conv.UnknownTaxIdFormatException;

public class SveaPmtAdminConverter {

	/**
	 * Prints a native order as a string.
	 * 
	 * @param order
	 * @return	The string representation of the native order.
	 */
	public static String nativeOrderToString(Order<?> order) {
		
		if (order.getNativeOrder()==null) return null;
		
		StringWriter sw = new StringWriter();
		
		if (order.getNativeOrder() instanceof org.notima.api.webpay.pmtapi.entity.Order) {
			
			org.notima.api.webpay.pmtapi.entity.Order nativeOrder = (org.notima.api.webpay.pmtapi.entity.Order) order.getNativeOrder();
			JAXB.marshal(nativeOrder, sw);			
			
		}
		
		return sw.toString();
		
	}
	
	@SuppressWarnings("rawtypes")
	public static Order<org.notima.api.webpay.pmtapi.CheckoutOrder> convert(org.notima.api.webpay.pmtapi.CheckoutOrder src) throws ParseException {
		if (src==null) return null;
		Order<org.notima.api.webpay.pmtapi.CheckoutOrder> dst = new Order<org.notima.api.webpay.pmtapi.CheckoutOrder>();
		
		org.notima.api.webpay.pmtapi.entity.Order s = src.getOrder();
		
		BusinessPartner<?> bp = new BusinessPartner();
		dst.setBpartner(bp);
		bp.setTaxId(s.getNationalId());
		bp.setCompany(s.isCompany());
		
		dst.setDocumentKey(Long.toString(s.getId()));
		dst.setOrderKey(s.getMerchantOrderId());
		dst.setCurrency(s.getCurrency());
		dst.setDateOrdered(PmtApiUtil.dateTimeFmt.parse(s.getCreatationDate()));
		
		Person person = new Person();
		if (s.isCompany())
			person.setName(s.getCustomerReference());
		else 
			person.setName(s.getBillingAddress().getFullName());
		
		person.setPhone(s.getPhoneNumber());
		person.setEmail(s.getEmailAddress());
		if (person.getName()==null || person.getName().trim().length()==0) {
			if (s.getBillingAddress()!=null)
					person.setName(s.getBillingAddress().getFullName());
		}
		
		dst.setBillPerson(person);
		
		dst.setBillLocation(convert(s.getBillingAddress()));
		dst.getBillLocation().setEmail(s.getEmailAddress());
		dst.getBillLocation().setPhone(s.getPhoneNumber());
		
		dst.setShipLocation(convert(s.getShippingAddress()));
		if (dst.getShipLocation()!=null) {
			dst.getShipLocation().setEmail(s.getEmailAddress());
		}
		
		// With card orders, billing address can be empty. Set billing adress to shipping adress in that case.
		if (s.getBillingAddress()==null || 
			(s.getBillingAddress().getFullName()==null && s.getBillingAddress().getStreetAddress()==null)) {
			dst.setBillLocation(dst.getShipLocation());
		}
		
		// Update bp to make bp complete by itself
		bp.setAddressOfficial(dst.getBillLocation());
		
		// Get name from either billing address or shipping address (if billing address is empty)
		bp.setName(s.getBillingAddress().getFullName()!=null ? s.getBillingAddress().getFullName() : s.getShippingAddress().getFullName());
		if (bp.isCompany()) {
			// Add contact
			List<Person> contacts = new ArrayList<Person>();
			contacts.add(person);
			bp.setContacts(contacts);
		}
		
		// Set payment rule from payment type
		dst.setPaymentRule(s.getPaymentType());
		
		List<OrderLine> ol = new ArrayList<OrderLine>();
		OrderLine ll;

		if (s.getDeliveries()!=null) {
			for (Delivery d : s.getDeliveries()) {
				// TODO: Perhaps make a more sophisticated delivery date check
				dst.setDateDelivered(PmtApiUtil.dateTimeFmt.parse(d.getCreationDate()));
				if (d.getOrderRows()!=null) {
					for (OrderRow r : d.getOrderRows()) {
						ll = convert(r);
						ll.setQtyDelivered(ll.getQtyEntered());
						ol.add(ll);
					}
				}
				
				// Only consider credits if everything is credited
				// If everything is credited we have no rows at all to calculate VAT.
				// TODO: Make a more in depth algorithm on this
				if (d.getDeliveryAmount().equals(d.getCreditedAmount())) {
				
					if (d.getCredits()!=null) {
						for (Credit cr : d.getCredits()) {
							if (cr.getOrderRows()!=null) {
								for (OrderRow creditedOrderRow : cr.getOrderRows()) {
									ll = convert(creditedOrderRow);
									// Set to zero delivered.
									ll.setQtyDelivered(0);
									ol.add(ll);
								}
							}
						}
					}
				}
			}
		}
		
		if (s.getOrderRows()!=null) {
			for (OrderRow r : s.getOrderRows()) {
				ll = convert(r);
				ol.add(ll);
			}
		}
		dst.setLines(ol);
		dst.calculateGrandTotal();

		// Check to see if OrgNo should be harmonized
		if (bp.isCompany() && "SE".equalsIgnoreCase(s.getBillingAddress().getCountryCode())) {
				try {
					bp.setTaxId(TaxIdFormatter.printTaxId(null, s.getNationalId(), TaxIdStructure.FMT_SE11));
				} catch (UnknownTaxIdFormatException e) {
				} catch (InvalidTaxIdFormatException e) {
				}
		}
		
		return dst;
	}
	
	public static Location convert(org.notima.api.webpay.pmtapi.entity.Address src) {
		if (src==null) return null;
		Location result = new Location();
		result.setStreet(src.getStreetAddress());
		result.setCo(src.getCoAddress());
		result.setPostal(src.getPostalCode());
		result.setCity(src.getCity());
		result.setCountryCode(src.getCountryCode());
		result.setCustomerReference(src.getFullName());
		return result;
	}
	
	public static OrderLine convert(org.notima.api.webpay.pmtapi.entity.OrderRow src) {
		
		OrderLine dst = new OrderLine();
		
		dst.setLineNo(Integer.valueOf(Long.toString(src.getOrderRowId())));
		dst.setKey(src.getArticleNumber());
		dst.setProductKey(src.getArticleNumber());
		dst.setName(src.getName());
		dst.setQtyEntered(src.getQuantity()/100);
		dst.setPriceActual(src.getUnitPrice()/100.0);
		dst.setPricesIncludeVAT(true);
		dst.setTaxPercent(src.getVatPercent()/100);
		dst.setUOM(src.getUnit());
		
		// TODO: Check discount
		
		// TODO: Handle isCancelled flag.
		
		return dst;
		
	}
	
}
