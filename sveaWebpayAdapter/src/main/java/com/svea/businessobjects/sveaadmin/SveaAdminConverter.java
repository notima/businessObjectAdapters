package com.svea.businessobjects.sveaadmin;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.BasicBusinessObjectConverter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.KeyValue;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderLine;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentWriteOff;
import org.notima.generic.businessobjects.PaymentWriteOffs;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.ifacebusinessobjects.OrderInvoiceLine;

import com.svea.businessobjects.SveaUtility;
import com.svea.webpay.common.conv.JsonUtil;
import com.svea.webpay.common.reconciliation.FeeDetail;
import com.svea.webpay.common.reconciliation.PaymentReportDetail;
import com.svea.webpay.common.reconciliation.PaymentReportGroup;
import com.svea.webpayadminservice.client.Address;
import com.svea.webpayadminservice.client.ArrayOfNumberedOrderRow;
import com.svea.webpayadminservice.client.ArrayOfOrderRow;
import com.svea.webpayadminservice.client.CompanyIdentity;
import com.svea.webpayadminservice.client.CreateOrderInformation;
import com.svea.webpayadminservice.client.CreateOrderRequest;
import com.svea.webpayadminservice.client.CreatePaymentPlanDetails;
import com.svea.webpayadminservice.client.CustomerIdentity;
import com.svea.webpayadminservice.client.CustomerType;
import com.svea.webpayadminservice.client.IndividualIdentity;
import com.svea.webpayadminservice.client.NumberedOrderRow;
import com.svea.webpayadminservice.client.OrderDeliveryStatus;
import com.svea.webpayadminservice.client.OrderRow;
import com.svea.webpayadminservice.client.OrderType;

/**
 * Converts to and from Svea Formats to Business Objects
 * 
 * @author Daniel Tamm
 *
 */
public class SveaAdminConverter {

	/**
	 * Creates an order request including payment plan details
	 * 
	 * 
	 * @param src		The request
	 * @param ot		Order type
	 * @param cpp		Payment Plan details
	 * @return			An Order request
	 * @throws Exception		If something goes wrong
	 */
	@SuppressWarnings("unchecked")
	public static CreateOrderRequest convert(Order<CreateOrderRequest> src, OrderType ot, CreatePaymentPlanDetails cpp) throws Exception {
		
		CreateOrderRequest req = new CreateOrderRequest();
		CreateOrderInformation dst = new CreateOrderInformation();
		req.setOrderInformation(dst);
		
		BusinessPartner<CustomerIdentity> bp = (BusinessPartner<CustomerIdentity>) src.getBillBpartner();
		if (bp!=null) {
			if (bp.getAddressOfficial()==null) {
				bp.setAddressOfficial(src.getBillLocation());
			}
			if (bp.getAddressShipping()==null) {
				bp.setAddressShipping(src.getShipLocation());
			}
		}
		
		Person p = src.getBillPerson();
		List<Person> contacts = bp.getContacts();
		
		// Check if there's a bill person specified on the order
		// Use the order bill person if set, otherwise,
		// the person / contact on the business partner.
		if (p!=null) {
			if (contacts==null || contacts.size()==0) {
				if (contacts==null) {
					contacts = new ArrayList<Person>();
					bp.setContacts(contacts);
				}
				contacts.add(p);
			} else {
				if (contacts.get(0)!=p) {
					contacts.add(0, p);
				}
			}
					
		} else {

			if (contacts==null || contacts.size()==0) {
				throw new Exception("Business Partner must have a contact to convert to CustomerIdentity");
			} else {
				p = contacts.get(0);
			}
			
		}
		
		dst.setClientOrderNumber(src.getDocumentKey());
		dst.setCustomerIdentity(convert(src.getBillBpartner()));
		dst.setOrderDeliveryAddress(convert(src.getShipLocation(), p));
		dst.setOrderDate(SveaUtility.getXMLDate(src.getDateOrdered()));
		dst.setCustomerReference(p.getName());
		
		// dst.setAddressSelector(value);
		
		ArrayOfOrderRow rows = new ArrayOfOrderRow();

		List<OrderRow> rl = rows.getOrderRow();
		
		for (OrderInvoiceLine ol : src.getOrderInvoiceLines()) {

			rl.add(convert(ol));
			
		}
		dst.setOrderRows(rows);
		
		dst.setOrderType(ot);
		if (cpp!=null) {
			dst.setCreatePaymentPlanDetails(cpp);
		}
		
		return req;
		
	}

	/**
	 * Converts Svea's webpay invoice to business object invoice
	 * 
	 * @param src	A webpay invoice.
	 * @return		An invoice in business objects format.
	 */
	public static org.notima.generic.businessobjects.Invoice<com.svea.webpayadminservice.client.Invoice> convert(com.svea.webpayadminservice.client.Invoice src) throws Exception {
		
		if (src==null) return null;
		
		org.notima.generic.businessobjects.Invoice<com.svea.webpayadminservice.client.Invoice> dst = new org.notima.generic.businessobjects.Invoice<com.svea.webpayadminservice.client.Invoice>(); 

		dst.setDocumentKey(src.getClientOrderId());
		dst.setCurrency(src.getCurrency());
		dst.setOrderKey(Long.toString(src.getSveaOrderId()));
		dst.setDocumentDate(src.getInvoiceDate().toGregorianCalendar().getTime());
		
		BusinessPartner<CustomerIdentity> bp = convert(src.getCustomer());
		dst.setBusinessPartner(bp);
		
		ArrayOfNumberedOrderRow orderArray = src.getInvoiceRows();
		List<NumberedOrderRow> rows = orderArray.getNumberedOrderRow();
		
		OrderLine ol = null;
		BasicBusinessObjectConverter<?,?> conv = new BasicBusinessObjectConverter();
		
		for (NumberedOrderRow r : rows) {
			// Only add non-cancelled rows
			if (!OrderDeliveryStatus.CANCELLED.value().equals(r.getStatus()))
				ol = convert(r);
				dst.addInvoiceLine(conv.toInvoiceLine(ol));
		}
		
		dst.calculateGrandTotal();
		dst.setNativeInvoice(src);
		
		return dst;
	}
	
	
	/**
	 * Converts Svea's webpay order to business object order
	 * 
	 * @param src	A webpay order.
	 * @return		An order in business objects format.
	 */
	public static org.notima.generic.businessobjects.Order<com.svea.webpayadminservice.client.Order> convert(com.svea.webpayadminservice.client.Order src) throws Exception {
		
		if (src==null) return null;
		
		org.notima.generic.businessobjects.Order<com.svea.webpayadminservice.client.Order> dst = new org.notima.generic.businessobjects.Order<com.svea.webpayadminservice.client.Order>(); 

		dst.setDocumentKey(src.getClientOrderId());
		dst.setCurrency(src.getCurrency());
		dst.setSalesOrder(true);
		dst.setDocumentDate(src.getCreatedDate().toGregorianCalendar().getTime());
		
		BusinessPartner<CustomerIdentity> bp = convert(src.getCustomer());
		
		dst.setBpartner(bp);
		
		// TODO: This number is wrong for payment plans
		if (!src.getOrderType().equals(OrderType.PAYMENT_PLAN.value())) {
			bp.setIdentityNo(Long.toString(src.getCustomerId()));
		}
		
		ArrayOfNumberedOrderRow orderArray = src.getOrderRows();
		List<NumberedOrderRow> rows = orderArray.getNumberedOrderRow();
		
		for (NumberedOrderRow r : rows) {
			// Only add non-cancelled rows
			if (!OrderDeliveryStatus.CANCELLED.value().equals(r.getStatus())) 
				dst.addOrderLine(convert(r));
		}
		
		dst.calculateGrandTotal();
		dst.setNativeOrder(src);
		
		return dst;
	}
	
	/**
	 * Converts a Svea order row
	 * 
	 * @param src
	 * @return
	 */
	public static OrderLine convert(NumberedOrderRow src) throws Exception {
		
		OrderLine dst = new OrderLine();
		
		dst.setProductKey(src.getArticleNumber());
		dst.setName(src.getDescription());
		dst.setKey(src.getArticleNumber());
		dst.setQtyEntered(src.getNumberOfUnits().doubleValue());
		dst.setUOM(src.getUnit());
		dst.setPricesIncludeVAT(src.isPriceIncludingVat());
		dst.setPriceActual(src.getPricePerUnit().doubleValue());
		dst.setTaxPercent(src.getVatPercent().doubleValue());
		
		if (src.getDiscountAmount().signum()!=0) {
			
			if (src.isDiscountAmountIncludingVat()) {
				if (src.isPriceIncludingVat()) {
					dst.setPriceNormal(dst.getPriceNormal() + src.getDiscountAmount().doubleValue());
				} else {
					// TODO: Create special calculation
					throw new Exception("Discount includes VAT but price is excluding VAT");
				}
			} else {
				if (!src.isPriceIncludingVat()) {
					dst.setPriceNormal(dst.getPriceNormal() + src.getDiscountAmount().doubleValue());
				} else {
					// TODO: Create special calculation
					throw new Exception("Price includes VAT but discount is excluding VAT");
				}
			}
			
		} else {
			dst.setPriceNormal(dst.getPriceActual());
		}

		dst.setLineNo((int)src.getRowNumber());
		dst.calculateLineTotalIncTax(2);
		
		return dst;
		
	}
	
	/**
	 * Converts one order row
	 * 
	 * @param src		The line
	 * @return			An order row
	 */
	public static OrderRow convert(OrderInvoiceLine src) {
		
		OrderRow dst = new OrderRow();
		
		dst.setArticleNumber(src.getProductKey());
		String description = src.getName();
		if (description!=null && description.trim().length()>0 && src.getDescription()!=null) {
			description += " " + src.getDescription();
		}
		dst.setDescription(description);
		dst.setNumberOfUnits(BigDecimal.valueOf(src.getQtyEntered()));
		dst.setUnit(src.getUOM());
		dst.setPricePerUnit(BigDecimal.valueOf(src.getPriceNormal()));
		dst.setVatPercent(BigDecimal.valueOf(src.getTaxPercent()));
		
		if (src.getPriceNormal()!=src.getPriceActual()) {
			dst.setDiscountAmount(BigDecimal.valueOf(src.getPriceNormal()-src.getPriceActual()));
			dst.setDiscountAmountIncludingVat(src.isPricesIncludeVAT());
		}
		dst.setPriceIncludingVat(src.isPricesIncludeVAT());
		
		// Use for line number
		dst.setTemporaryReference(Integer.toString(src.getLineNo()));
		
		return dst;
		
		
	}
	
	
	/**
	 * Converts a location and person into a Svea Address.
	 * Use co, street and house number to create the adress.
	 * 
	 * @param src		A location object
	 * @param p			The person
	 * @return			A Svea address structure.
	 */
	public static Address convert(Location src, Person p) {
		Address dst = new Address();
		
		dst.setCoAddress(src.getCo());
		if (src.getStreet()!=null)
			dst.setStreet(src.getStreet());
		else 
			dst.setStreet(src.getAddress1());
		
		dst.setHouseNumber(src.getHouseNo());
		dst.setZipCode(src.getPostal());
		dst.setLocality(src.getCity());
		dst.setCountryCode(src.getCountryCode());
		
		if (p!=null) {
			dst.setFirstName(p.getFirstName());
			dst.setLastName(p.getLastName());
			dst.setFullName(p.getName());
		}
		
		return dst;
		
	}
	
	/**
	 * Converts a customer identity to a business partner object
	 * 
	 * @param src			The customer identity
	 * @return				A business partner customer.
	 * @throws Exception	If something goes wrong
	 */
	public static BusinessPartner<CustomerIdentity> convert(CustomerIdentity src) throws Exception {
	
		BusinessPartner<CustomerIdentity> dst = new BusinessPartner<CustomerIdentity>();
		dst.setCompany(CustomerType.COMPANY.equals(src.getCustomerType()));

		dst.setIdentityNo(src.getNationalIdNumber());
		dst.setTaxId(src.getNationalIdNumber());
		// Truncate "16" prefix if present.
		if (dst.isCompany() && dst.getTaxId()!=null && dst.getTaxId().startsWith("16")) {
			dst.setTaxId(dst.getTaxId().substring(2));
		}
		dst.setName(src.getFullName());

		dst.setIsCustomer(true);
		
		Location address = new Location();
		address.setCo(src.getCoAddress());
		address.setStreet(src.getStreet());
		address.setHouseNo(src.getHouseNumber());
		address.setPostal(src.getZipCode());
		address.setCity(src.getLocality());
		address.setEmail(src.getEmail());
		address.setPhone(src.getPhoneNumber());
		dst.setAddressOfficial(address);
		
		return dst;
		
	}
	
	/**
	 * Use co, street and house number to create the address (don't use address1-4).
	 * Set contact - attribute - birthDate to include birth date of
	 * individual identity.
	 * 
	 * @param bp			The business partner
	 * @return				A customer identity (Svea Format)
	 * @throws Exception	If something goes wrong
	 */
	public static CustomerIdentity convert(BusinessPartner<?> bp) throws Exception {
		
		CustomerIdentity dst = new CustomerIdentity();
		dst.setNationalIdNumber(bp.getTaxId());
		List<Person> contacts = bp.getContacts();
		
		if (contacts==null || contacts.size()<1) 
			throw new Exception("Business Partner must have a contact to convert to CustomerIdentity");
		
		Person contact = contacts.get(0);
		dst.setEmail(contact.getEmail());
		dst.setPhoneNumber(contact.getPhone());
		dst.setFullName(bp.getName());
		
		// Get address
		Location address = bp.getAddressOfficial();
		if (address!=null) {
			dst.setCoAddress(address.getCo());
			if (address.getStreet()!=null)
				dst.setStreet(address.getStreet());
			else
				dst.setStreet(address.getAddress1());
			dst.setHouseNumber(address.getHouseNo());
			dst.setZipCode(address.getPostal());
			dst.setLocality(address.getCity());
			dst.setCountryCode(address.getCountryCode());
		}
		
		if (bp.isCompany()) {
			CompanyIdentity ci = new CompanyIdentity();
			ci.setCompanyIdentification(bp.getTaxId());
			ci.setCompanyVatNumber(bp.getVatNo());
			dst.setCompanyIdentity(ci);
			dst.setCustomerType(CustomerType.COMPANY);
			if (dst.getCoAddress()==null || dst.getCoAddress().trim().length()==0) {
				dst.setCoAddress(contact.getName());
			}
		} else {
			IndividualIdentity ii = new IndividualIdentity();
			ii.setFirstName(contact.getFirstName());
			ii.setLastName(contact.getLastName());
			if (contact.getAttributes()!=null) {
				for (KeyValue kv : contact.getAttributes()) {
					if ("birthDate".equalsIgnoreCase(kv.getKey())) {
						ii.setBirthDate(kv.getValue());
					}
				}
			}
			dst.setIndividualIdentity(ii);
			dst.setCustomerType(CustomerType.INDIVIDUAL);
		}

		return dst;
		
	}
	
	/**
	 * Converts a payment report group to a list of payments (business objects format).
	 * 
	 * @param group			A payment report group
	 * @param retriesOnly	Only convert payments marked with retry.
	 * @return				A list of payments.
	 * @throws ParseException	If report can't be parsed.
	 */
	public List<Payment<PaymentReportDetail>> convert(PaymentReportGroup group, boolean retriesOnly, boolean includeFees) throws ParseException {
		
		List<Payment<PaymentReportDetail>> dstList = new ArrayList<Payment<PaymentReportDetail>>();
		Payment<PaymentReportDetail> p;
		
		if (group==null || group.getPaymentReportDetail()==null) return dstList;
		
		for (PaymentReportDetail d : group.getPaymentReportDetail()) {
			if (d==null) continue;
			if ((retriesOnly && d.getRetry()!=null && d.getRetry().booleanValue())
					|| !retriesOnly) {
				p = convert(d, group, includeFees);
				p.setNativePayment(d);
				dstList.add(p);
			}
		}
		
		return dstList;
		
	}
	
	
	/**
	 * Converts a payment report detail in given group to a single payment
	 * 
	 * @param src			The payment to be converted
	 * @param group			The group of the payment (src)
	 * @return				A payment
	 * @throws ParseException	If something goes wrong
	 */
	public Payment convert(PaymentReportDetail src, PaymentReportGroup group, boolean includeFees) throws ParseException {
		
		Payment dst = new Payment();
		
		dst.setPaymentDate(JsonUtil.dfmt.parse(group.getReconciliationDate()));
		dst.setCurrency(group.getCurrency());
		
		dst.setAccountNo(group.getDstAcct());
		dst.setAmount(includeFees ? src.getReceivedAmt() : src.getPaidAmt());
		dst.setBusinessPartnerKey(src.getCustomerId());
		dst.setInvoiceNo(src.getInvoiceId());
		BusinessPartner bp = new BusinessPartner();
		dst.setBusinessPartner(bp);
		bp.setName(src.getPayerName());
		bp.setIdentityNo(src.getPayerOrgNo());
		// Use Payment ID if there's no order ID.
		dst.setOrderNo(src.getOrderId()!=null && src.getOrderId().trim().length()>0 ? src.getOrderId() : src.getPaymentId());
		dst.setClientOrderNo(src.getClientOrderNo());
		
		// Special for credit card
		if (dst.getInvoiceNo()==null && dst.getOrderNo()==null && src.getClientOrderNo()!=null) {
			dst.setInvoiceNo(src.getClientOrderNo());
		}

		if (includeFees && src.getPaidAmt()!=src.getReceivedAmt()) {

			double vatOnPaymentFee = 0;
			
			// Write offs
			PaymentWriteOff pw;
			if (src.getFees()!=null) {
				for (FeeDetail fd : src.getFees()) {
	
					// Create list of write-off if it doesn't exist yet
					if (dst.getPaymentWriteOffs()==null) {
						dst.setPaymentWriteOffs(new PaymentWriteOffs());
						List<PaymentWriteOff> list = new ArrayList<PaymentWriteOff>();
						dst.getPaymentWriteOffs().setPaymentWriteOff(list);
					}
					
					pw = new PaymentWriteOff();
					pw.setAccountNo(fd.getAccountNr());
					pw.setAmount(fd.getFee());
					
					dst.getPaymentWriteOffs().getPaymentWriteOff().add(pw);
					
					if (fd.getFeeVat()!=null && fd.getFeeVat()!=0) {
						vatOnPaymentFee += fd.getFeeVat();
					}
					
				}
			}
			
			if (vatOnPaymentFee!=0) {
				pw = new PaymentWriteOff();
				pw.setAccountNo(group.getVatAcct());
				pw.setAmount(vatOnPaymentFee);
				dst.getPaymentWriteOffs().getPaymentWriteOff().add(pw);
			}
				
		}
		
		return dst;
		
	}
	
}
