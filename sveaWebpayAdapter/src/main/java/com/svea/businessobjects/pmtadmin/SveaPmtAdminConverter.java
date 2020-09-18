package com.svea.businessobjects.pmtadmin;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderLine;
import org.notima.generic.businessobjects.Person;

import com.svea.webpay.common.conv.InvalidTaxIdFormatException;
import com.svea.webpay.common.conv.TaxIdFormatter;
import com.svea.webpay.common.conv.TaxIdStructure;
import com.svea.webpay.common.conv.UnknownTaxIdFormatException;

import org.notima.api.webpay.pmtapi.PmtApiUtil;
import org.notima.api.webpay.pmtapi.entity.Delivery;
import org.notima.api.webpay.pmtapi.entity.OrderRow;

public class SveaPmtAdminConverter {

	@SuppressWarnings("rawtypes")
	public static Order<org.notima.api.webpay.pmtapi.entity.Order> convert(org.notima.api.webpay.pmtapi.entity.Order src) throws ParseException {
		if (src==null) return null;
		Order<org.notima.api.webpay.pmtapi.entity.Order> dst = new Order<org.notima.api.webpay.pmtapi.entity.Order>();
		
		BusinessPartner<?> bp = new BusinessPartner();
		dst.setBpartner(bp);
		bp.setTaxId(src.getNationalId());
		bp.setCompany(src.isCompany());
		
		dst.setDocumentKey(Long.toString(src.getId()));
		dst.setOrderKey(src.getMerchantOrderId());
		dst.setCurrency(src.getCurrency());
		dst.setDateOrdered(PmtApiUtil.dateTimeFmt.parse(src.getCreatationDate()));
		
		Person person = new Person();
		if (src.isCompany())
			person.setName(src.getCustomerReference());
		else 
			person.setName(src.getBillingAddress().getFullName());
		
		person.setPhone(src.getPhoneNumber());
		person.setEmail(src.getEmailAddress());
		if (person.getName()==null || person.getName().trim().length()==0) {
			if (src.getBillingAddress()!=null)
					person.setName(src.getBillingAddress().getFullName());
		}
		
		dst.setBillPerson(person);
		
		dst.setBillLocation(convert(src.getBillingAddress()));
		dst.getBillLocation().setEmail(src.getEmailAddress());
		dst.getBillLocation().setPhone(src.getPhoneNumber());
		
		dst.setShipLocation(convert(src.getShippingAddress()));
		if (dst.getShipLocation()!=null) {
			dst.getShipLocation().setEmail(src.getEmailAddress());
		}
		
		// With card orders, billing address can be empty. Set billing adress to shipping adress in that case.
		if (src.getBillingAddress()==null || 
			(src.getBillingAddress().getFullName()==null && src.getBillingAddress().getStreetAddress()==null)) {
			dst.setBillLocation(dst.getShipLocation());
		}
		
		// Update bp to make bp complete by itself
		bp.setAddressOfficial(dst.getBillLocation());
		
		// Get name from either billing address or shipping address (if billing address is empty)
		bp.setName(src.getBillingAddress().getFullName()!=null ? src.getBillingAddress().getFullName() : src.getShippingAddress().getFullName());
		if (bp.isCompany()) {
			// Add contact
			List<Person> contacts = new ArrayList<Person>();
			contacts.add(person);
			bp.setContacts(contacts);
		}
		
		// Set payment rule from payment type
		dst.setPaymentRule(src.getPaymentType());
		
		List<OrderLine> ol = new ArrayList<OrderLine>();
		OrderLine ll;

		if (src.getDeliveries()!=null) {
			for (Delivery d : src.getDeliveries()) {
				// TODO: Perhaps make a more sophisticated delivery date check
				dst.setDateDelivered(PmtApiUtil.dateTimeFmt.parse(d.getCreationDate()));
				if (d.getOrderRows()!=null) {
					for (OrderRow r : d.getOrderRows()) {
						ll = convert(r);
						ll.setQtyDelivered(ll.getQtyEntered());
						ol.add(ll);
					}
				}
			}
		}
		
		if (src.getOrderRows()!=null) {
			for (OrderRow r : src.getOrderRows()) {
				ll = convert(r);
				ol.add(ll);
			}
		}
		dst.setLines(ol);
		dst.calculateGrandTotal();

		// Check to see if OrgNo should be harmonized
		if (bp.isCompany() && "SE".equalsIgnoreCase(src.getBillingAddress().getCountryCode())) {
				try {
					bp.setTaxId(TaxIdFormatter.printTaxId(null, src.getNationalId(), TaxIdStructure.FMT_SE11));
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
