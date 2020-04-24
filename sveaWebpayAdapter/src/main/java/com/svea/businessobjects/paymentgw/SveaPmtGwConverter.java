package com.svea.businessobjects.paymentgw;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderLine;
import org.notima.generic.businessobjects.Person;

import com.svea.webpay.paymentgw.entity.Customer;
import com.svea.webpay.paymentgw.entity.Row;
import com.svea.webpay.paymentgw.entity.Transaction;

public class SveaPmtGwConverter {

	public static Order<Transaction> convert(Transaction src) throws ParseException {
		if (src==null) return null;
		Order<Transaction> dst = new Order<Transaction>();

		dst.setDocumentKey(Long.toString(src.getId()));
		dst.setOrderKey(src.getCustomerRefNo());
		dst.setCurrency(src.getCurrency());
		dst.setDateOrdered(src.getCreated());
		
		BusinessPartner<Customer> bp = convert(src.getCustomer());
		dst.setBpartner(bp);

		// Set payment rule from payment type
		dst.setPaymentRule(src.getPaymentMethod());
		
		List<OrderLine> ol = new ArrayList<OrderLine>();
		OrderLine ll;
		
		if (src.getOrderRows()!=null && src.getOrderRows().getOrderRows()!=null) {
			for (Row r : src.getOrderRows().getOrderRows()) {
				ll = convert(r);
				ol.add(ll);
			}
		}
		dst.setLines(ol);
		dst.calculateGrandTotal();
		
		return dst;
	}
	
	public static BusinessPartner<Customer> convert(com.svea.webpay.paymentgw.entity.Customer src) {
		
		BusinessPartner<Customer> bp = new BusinessPartner<Customer>();
		
		if (src==null) return null;
		bp.setIdentityNo(src.getId().toString());
		bp.setCompany(src.getIsCompany());
		if (bp.isCompany()) {
			bp.setName(src.getCompanyName());
		} else {
			bp.setName(src.getFullName());
		}
		
		bp.setTaxId(src.getSsn());
		bp.setVatNo(src.getVatNumber());

		Person p = new Person();
		p.setName(src.getFullName());
		p.setFirstName(src.getFirstName());
		p.setLastName(src.getLastName());
		p.setEmail(src.getEmail());
		List<Person> contacts = new ArrayList<Person>();
		contacts.add(p);
		bp.setContacts(contacts);
		
		Location address = convertLocation(src);
		bp.setAddressOfficial(address);
		
		return bp;
		
	}
	
	public static Location convertLocation(com.svea.webpay.paymentgw.entity.Customer src) {
		if (src==null) return null;
		Location result = new Location();
		result.setAddress1(src.getAddress());
		result.setAddress2(src.getAddress2());
		result.setPostal(src.getZip());
		result.setCity(src.getCity());
		result.setCountryCode(src.getCountry());
		return result;
	}
	
	public static OrderLine convert(com.svea.webpay.paymentgw.entity.Row src) {
		
		OrderLine dst = new OrderLine();
		if (src.getId()!=null) {
			try {
				dst.setLineNo(Integer.parseInt(src.getId()));
			} catch (NumberFormatException ne) {
				dst.setKey(src.getId());
			}
		}
		if (dst.getKey()==null)
			dst.setKey(src.getSKU());
		dst.setProductKey(src.getSKU());
		dst.setName(src.getName());
		dst.setDescription(src.getDescription());
		dst.setQtyEntered(src.getQuantity());
		dst.setPriceActual(src.getAmount()/100.0);
		dst.setPricesIncludeVAT(true);
		dst.setTaxAmount(src.getVat()/100.0);
		dst.calculateTaxPercent(dst.getPriceActual());
		dst.setUOM(src.getUnit());
		
		return dst;
		
	}
	
}
