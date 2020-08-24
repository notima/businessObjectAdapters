package org.notima.generic.ubl.factory;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.datatype.XMLGregorianCalendar;

import org.notima.generic.businessobjects.BasicBusinessObjectConverter;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.util.InvalidTaxIdFormatException;
import org.notima.generic.businessobjects.util.TaxIdStructure;
import org.notima.generic.businessobjects.util.UnknownTaxIdFormatException;

import com.helger.ubl21.UBL21Reader;
import com.helger.ubl21.UBL21Writer;
import com.phloc.datetime.xml.PDTXMLConverter;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.BranchType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CountryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CreditNoteLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.FinancialAccountType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PriceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.CreditedQuantityType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.EndpointIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.InvoicedQuantityType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PaymentIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PaymentMeansCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.PriceAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxableAmountType;
import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

/**
 * Helper class to convert business objects to UBL objects.
 * 
 * @author daniel
 *
 */
public class UBL21Converter extends BasicBusinessObjectConverter<Object, InvoiceType> {

	public static final String BIS30_CUSTOMIZATION_ID="urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0";
	public static final String BIS30_PROFILE_ID="urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";
	
	/**
	 * Converts a business objects location to an UBL AddressType.
	 * 
	 * 
	 * @param loc
	 * @return
	 */
	public static AddressType convert(Location loc) {
		if (loc == null)
			return null;
		AddressType at = new AddressType();

		if (loc.getAddress1()!=null && loc.getAddress1().trim().length()>0)
			at.setStreetName(loc.getAddress1());
		if (loc.getAddress2()!=null && loc.getAddress2().trim().length()>0)
			at.setAdditionalStreetName(loc.getAddress2());
		at.setCityName(loc.getCity());
		at.setPostalZone(loc.getPostal());
		CountryType ct = new CountryType();
		ct.setIdentificationCode(loc.getCountryCode());
		at.setCountry(ct);
		
		return at;
	}
	
	public static InvoiceType addPaymentMeansBankgiro(InvoiceType dst, String bgAccount, String ref, String accountName) {
		
		dst = addPaymentMeans(dst, "30", ref, bgAccount, "SE:BANKGIRO", accountName);
		
		return dst;
	}
	
	/**
	 * Returns the sender id (normally the org number)
	 * @param dst
	 * @return
	 */
	public static EndpointIDType getSenderId(InvoiceType dst) {
	
		EndpointIDType result = null;
		
		if (dst.getAccountingSupplierParty()!=null 
				&& dst.getAccountingSupplierParty().getParty()!=null 
				&& dst.getAccountingSupplierParty().getParty().getEndpointID()!=null)
			
		result = dst.getAccountingSupplierParty().getParty().getEndpointID();
		
		return result;
	}
	
	/**
	 * Returns the customer id (normally the org number)
	 * @param dst
	 * @return
	 */
	public static EndpointIDType getCustomerId(InvoiceType dst) {
		
		EndpointIDType result = null;
		
		if (dst.getAccountingCustomerParty()!=null 
				&& dst.getAccountingCustomerParty().getParty()!=null 
				&& dst.getAccountingCustomerParty().getParty().getEndpointID()!=null)
			
		result = dst.getAccountingCustomerParty().getParty().getEndpointID();
		
		return result;
	}
	
	/**
	 * Returns the sender id (normally the org number)
	 * @param dst
	 * @return
	 */
	public static EndpointIDType getCreditNoteSenderId(CreditNoteType dst) {
	
		EndpointIDType result = null;
		
		if (dst.getAccountingSupplierParty()!=null 
				&& dst.getAccountingSupplierParty().getParty()!=null 
				&& dst.getAccountingSupplierParty().getParty().getEndpointID()!=null)
			
		result = dst.getAccountingSupplierParty().getParty().getEndpointID();
		
		return result;
	}
	
	/**
	 * Returns the customer id (normally the org number)
	 * @param dst
	 * @return
	 */
	public static EndpointIDType getCreditNoteCustomerId(CreditNoteType dst) {
		
		EndpointIDType result = null;
		
		if (dst.getAccountingCustomerParty()!=null 
				&& dst.getAccountingCustomerParty().getParty()!=null 
				&& dst.getAccountingCustomerParty().getParty().getEndpointID()!=null)
			
		result = dst.getAccountingCustomerParty().getParty().getEndpointID();
		
		return result;
	}
	
	/**
	 * Adds a payment means to invoice.
	 * No check for duplicates is made.
	 * 
	 * @param dst
	 * @param meansCode
	 * @param paymentId
	 * @param idAccount
	 * @param financialInstitutionBranchId
	 * @return
	 */
	public static InvoiceType addPaymentMeans(InvoiceType dst, 
							String meansCode, 
							String paymentId, 
							String idAccount, 
							String financialInstitutionBranchId,
							String accountName) {

		List<PaymentMeansType> list = dst.getPaymentMeans();
		if (list==null) {
			list = new ArrayList<PaymentMeansType>();
			dst.setPaymentMeans(list);
		}

		PaymentMeansType pmt = new PaymentMeansType();
		PaymentMeansCodeType pmct = new PaymentMeansCodeType();
		pmct.setValue(meansCode);
		pmt.setPaymentMeansCode(pmct);
		List<PaymentIDType> pitlist = new ArrayList<PaymentIDType>();
		PaymentIDType pit = new PaymentIDType();
		pit.setValue(paymentId);
		pitlist.add(pit);
		pmt.setPaymentID(pitlist);;

		FinancialAccountType pfa = new FinancialAccountType();
		pfa.setID(idAccount);
		pfa.setName(accountName);
		BranchType bt = new BranchType();
		bt.setID(financialInstitutionBranchId);
		pfa.setFinancialInstitutionBranch(bt);
		pmt.setPayeeFinancialAccount(pfa);
		list.add(pmt);
		
		return dst;
	}
	
	public static CreditNoteType convertToCreditNote(Invoice src) {
		
		CreditNoteType dst = new CreditNoteType();
		
		int roundingDecimals = src.getRoundingDecimals();
		
		dst.setCustomizationID(BIS30_CUSTOMIZATION_ID);
		dst.setProfileID(BIS30_PROFILE_ID);
		
		dst.setID(src.getInvoiceKey());
		XMLGregorianCalendar invoiceDate = PDTXMLConverter.getXMLCalendar(src.getInvoiceDate());
		invoiceDate.setTimezone(javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED);
		dst.setIssueDate(invoiceDate);
		
		// InvoiceTypeCode
		dst.setCreditNoteTypeCode("381");
		dst.setDocumentCurrencyCode(src.getCurrency());
		if (src.getPoDocumentNo()!=null && src.getPoDocumentNo().trim().length()>0) {
			dst.setBuyerReference(src.getPoDocumentNo());
		} else {
			if (src.getBillPerson()!=null) {
				dst.setBuyerReference(src.getBillPerson().getName());
			} else {
				dst.setBuyerReference("-");
			}
		}
		
		// Supplier
		// =======
		SupplierPartyType supplier = new SupplierPartyType();
		dst.setAccountingSupplierParty(supplier);
		PartyType sp = new PartyType();
		supplier.setParty(sp);

		Location officalAddress = src.getSender().getAddressOfficial();
		
		// Seller's electronic address
		EndpointIDType sep = new EndpointIDType();
		
		if ("SE".equalsIgnoreCase(officalAddress.getCountryCode())) {
			sep.setSchemeID("0007");
			try {
				sep.setValue(src.getSender().getTaxId(TaxIdStructure.FMT_SE10));
			} catch (UnknownTaxIdFormatException | InvalidTaxIdFormatException e) {
				sep.setValue(src.getSender().getTaxId());
			}
		}
		
		sp.setEndpointID(sep);

		PartyNameType pnt = new PartyNameType();
		pnt.setName(src.getSender().getName());
		List<PartyNameType> pnts = new ArrayList<PartyNameType>();
		pnts.add(pnt);
		sp.setPartyName(pnts);

		AddressType at = convert(officalAddress);
		sp.setPostalAddress(at);
		
		PartyTaxSchemeType ptst = new PartyTaxSchemeType();
		List<PartyTaxSchemeType> ptsts = new ArrayList<PartyTaxSchemeType>();
		ptsts.add(ptst);
		sp.setPartyTaxScheme(ptsts);
		ptst.setCompanyID(src.getSender().getVatNo());
		
		TaxSchemeType tst = new TaxSchemeType();
		tst.setID("VAT");
		ptst.setTaxScheme(tst);
		
		PartyLegalEntityType plet = new PartyLegalEntityType();
		plet.setRegistrationName(src.getSender().getName());
		List<PartyLegalEntityType> plets = new ArrayList<PartyLegalEntityType>();
		plets.add(plet);
		sp.setPartyLegalEntity(plets);
		
		// Customer
		// ========
		CustomerPartyType customer = new CustomerPartyType();
		dst.setAccountingCustomerParty(customer);
		sp = new PartyType();
		customer.setParty(sp);
		
		// Customer's electronic address
		sep = new EndpointIDType();
		
		if ("SE".equalsIgnoreCase(officalAddress.getCountryCode())) {
			sep.setSchemeID("0007");
			try {
				sep.setValue(src.getBillBpartner().getTaxId(TaxIdStructure.FMT_SE10));
			} catch (UnknownTaxIdFormatException | InvalidTaxIdFormatException e) {
				sep.setValue(src.getBillBpartner().getTaxId());
			}
		}
		
		sp.setEndpointID(sep);
		
		pnt = new PartyNameType();
		pnt.setName(src.getBillBpartner().getName());
		pnts = new ArrayList<PartyNameType>();
		pnts.add(pnt);
		sp.setPartyName(pnts);
		
		at = convert(src.getBillLocation());
		sp.setPostalAddress(at);

		plet = new PartyLegalEntityType();
		plet.setRegistrationName(src.getBillBpartner().getName());
		plets = new ArrayList<PartyLegalEntityType>();
		plets.add(plet);
		sp.setPartyLegalEntity(plets);
		
		// Prepare for building a tax structure from the invoice lines
		Map<Double, TaxSubtotalType> tsm = new TreeMap<Double,TaxSubtotalType>();
		
		CreditNoteLineType line = null;
		
		InvoiceLine il;
		double lineExtAmt = 0, taxAmt = 0;
		InvoiceLine adjustment = null;
		
		for (Object oo : src.getLines()) {
			
			il = (InvoiceLine)oo;
		
			if (Math.abs(il.getLineNet())<1 && il.getTaxAmount()==0) {
				// We have an adjustment line
				adjustment = il;
				continue;
			}
			
			line = convertCreditNoteLine(il, src.getCurrency(), tsm);
			lineExtAmt += il.getLineNet();
			taxAmt += il.getTaxAmount();
			
			dst.addCreditNoteLine(line);
			
			
		}
		
		lineExtAmt = UBL21Converter.round(lineExtAmt, roundingDecimals);

		// Calculate adjustment
		double adjustmentAmt = src.getGrandTotal() - taxAmt - lineExtAmt;
		adjustmentAmt = UBL21Converter.round(adjustmentAmt, roundingDecimals);

		if (adjustment!=null || adjustmentAmt!=0) {
			
			if (adjustment==null) {
				adjustment = new InvoiceLine();
				adjustment.setName("Rounding");
			}
			adjustment.setLineNet(adjustmentAmt);
			adjustment.setPriceActual(adjustmentAmt);

			line = convertCreditNoteLine(adjustment, src.getCurrency(), tsm);
			lineExtAmt += adjustment.getLineNet();
			dst.addCreditNoteLine(line);
			
		}
		
		// Tax amounts
		// ===========
		TaxTotalType ttt = new TaxTotalType();
		TaxAmountType tat = new TaxAmountType();
		taxAmt = UBL21Converter.round(taxAmt, roundingDecimals);
		tat.setValue(BigDecimal.valueOf(taxAmt));
		tat.setCurrencyID(src.getCurrency());
		ttt.setTaxAmount(tat);
		List<TaxTotalType> ttts = new ArrayList<TaxTotalType>();
		ttts.add(ttt);
		dst.setTaxTotal(ttts);
		
		// Add tax subtotals
		for (TaxSubtotalType tsubtotal : tsm.values()) {
			ttt.addTaxSubtotal(tsubtotal);
		}
		
		// Invoice amounts
		
		MonetaryTotalType aMT = new MonetaryTotalType();
		
		aMT.setPayableAmount(BigDecimal.valueOf(src.getGrandTotal())).setCurrencyID(src.getCurrency());
		aMT.setPrepaidAmount(BigDecimal.ZERO).setCurrencyID(src.getCurrency());
		
		
		aMT.setLineExtensionAmount(BigDecimal.valueOf(lineExtAmt)).setCurrencyID(src.getCurrency());
		aMT.setTaxExclusiveAmount(BigDecimal.valueOf(src.getGrandTotal()-taxAmt)).setCurrencyID(src.getCurrency());
		aMT.setTaxInclusiveAmount(BigDecimal.valueOf(src.getGrandTotal())).setCurrencyID(src.getCurrency());

		dst.setLegalMonetaryTotal(aMT);
		
		return dst;
		
	}
	
	@Override
	public InvoiceType toNativeInvoice(Invoice<InvoiceType> src) throws Exception {
		return convert(src);
	}

	/**
	 * Converts a business objects invoice to an UBL InvoiceType
	 * 
	 * @param src
	 * @return
	 */
	public static InvoiceType convert(Invoice src) {

		InvoiceType dst = new InvoiceType();

		int roundingDecimals = src.getRoundingDecimals();
		
		dst.setCustomizationID(BIS30_CUSTOMIZATION_ID);
		dst.setProfileID(BIS30_PROFILE_ID);
		
		dst.setID(src.getInvoiceKey());
		XMLGregorianCalendar invoiceDate = PDTXMLConverter.getXMLCalendar(src.getInvoiceDate());
		invoiceDate.setTimezone(javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED);
		dst.setIssueDate(invoiceDate);

		XMLGregorianCalendar dueDate = PDTXMLConverter.getXMLCalendar(src.getDueDate());
		dueDate.setTimezone(javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED);
		dst.setDueDate(dueDate);
		
		// InvoiceTypeCode
		dst.setInvoiceTypeCode("380");
		dst.setDocumentCurrencyCode(src.getCurrency());
		if (src.getPoDocumentNo()!=null && src.getPoDocumentNo().trim().length()>0) {
			dst.setBuyerReference(src.getPoDocumentNo());
		} else {
			if (src.getBillPerson()!=null) {
				dst.setBuyerReference(src.getBillPerson().getName());
			} else {
				dst.setBuyerReference("-");
			}
		}
		
		// Supplier
		// =======
		SupplierPartyType supplier = new SupplierPartyType();
		dst.setAccountingSupplierParty(supplier);
		PartyType sp = new PartyType();
		supplier.setParty(sp);

		Location officalAddress = src.getSender().getAddressOfficial();
		
		// Seller's electronic address
		EndpointIDType sep = new EndpointIDType();
		
		if ("SE".equalsIgnoreCase(officalAddress.getCountryCode())) {
			sep.setSchemeID("0007");
			try {
				sep.setValue(src.getSender().getTaxId(TaxIdStructure.FMT_SE10));
			} catch (UnknownTaxIdFormatException | InvalidTaxIdFormatException e) {
				sep.setValue(src.getSender().getTaxId());
			}
		}
		
		sp.setEndpointID(sep);

		PartyNameType pnt = new PartyNameType();
		pnt.setName(src.getSender().getName());
		List<PartyNameType> pnts = new ArrayList<PartyNameType>();
		pnts.add(pnt);
		sp.setPartyName(pnts);

		AddressType at = convert(officalAddress);
		sp.setPostalAddress(at);
		
		PartyTaxSchemeType ptst = new PartyTaxSchemeType();
		List<PartyTaxSchemeType> ptsts = new ArrayList<PartyTaxSchemeType>();
		ptsts.add(ptst);
		sp.setPartyTaxScheme(ptsts);
		ptst.setCompanyID(src.getSender().getVatNo());
		
		TaxSchemeType tst = new TaxSchemeType();
		tst.setID("VAT");
		ptst.setTaxScheme(tst);
		
		PartyLegalEntityType plet = new PartyLegalEntityType();
		plet.setRegistrationName(src.getSender().getName());
		List<PartyLegalEntityType> plets = new ArrayList<PartyLegalEntityType>();
		plets.add(plet);
		sp.setPartyLegalEntity(plets);
		
		// Customer
		// ========
		CustomerPartyType customer = new CustomerPartyType();
		dst.setAccountingCustomerParty(customer);
		sp = new PartyType();
		customer.setParty(sp);
		
		// Customer's electronic address
		sep = new EndpointIDType();
		
		if ("SE".equalsIgnoreCase(officalAddress.getCountryCode())) {
			sep.setSchemeID("0007");
			try {
				sep.setValue(src.getBillBpartner().getTaxId(TaxIdStructure.FMT_SE10));
			} catch (UnknownTaxIdFormatException | InvalidTaxIdFormatException e) {
				sep.setValue(src.getBillBpartner().getTaxId());
			}
		}
		
		sp.setEndpointID(sep);
		
		pnt = new PartyNameType();
		pnt.setName(src.getBillBpartner().getName());
		pnts = new ArrayList<PartyNameType>();
		pnts.add(pnt);
		sp.setPartyName(pnts);
		
		at = convert(src.getBillLocation());
		sp.setPostalAddress(at);

		plet = new PartyLegalEntityType();
		plet.setRegistrationName(src.getBillBpartner().getName());
		plets = new ArrayList<PartyLegalEntityType>();
		plets.add(plet);
		sp.setPartyLegalEntity(plets);
		
		// Prepare for building a tax structure from the invoice lines
		Map<Double, TaxSubtotalType> tsm = new TreeMap<Double,TaxSubtotalType>();
		
		InvoiceLineType line = null;
		
		InvoiceLine il;
		double lineExtAmt = 0, taxAmt = 0;
		InvoiceLine adjustment = null;
		
		for (Object oo : src.getLines()) {
			
			il = (InvoiceLine)oo;
		
			if (adjustment==null && il.getLineNo()>=10 && Math.abs(il.getLineNet())<1 && il.getTaxAmount()==0 && il.getQtyEntered()!=0) {
				// We have an adjustment line
				adjustment = il;
				continue;
			}
			
			line = convertInvoiceLine(il, src.getCurrency(), tsm);
			lineExtAmt += il.getLineNet();
			taxAmt += il.getTaxAmount();
			
			dst.addInvoiceLine(line);
			
		}
		
		lineExtAmt = UBL21Converter.round(lineExtAmt, roundingDecimals);

		// Calculate adjustment
		double adjustmentAmt = src.getGrandTotal() - taxAmt - lineExtAmt;
		adjustmentAmt = UBL21Converter.round(adjustmentAmt, roundingDecimals);

		if (adjustment!=null || adjustmentAmt!=0) {
			
			if (adjustment==null) {
				adjustment = new InvoiceLine();
				adjustment.setName("Rounding");
			}
			adjustment.setLineNet(adjustmentAmt);
			adjustment.setPriceActual(Math.abs(adjustmentAmt));
			adjustment.setQtyEntered(adjustmentAmt>=0 ? 1.0 : -1.0);
			adjustment.setUOM("EA");

			line = convertInvoiceLine(adjustment, src.getCurrency(), tsm);
			lineExtAmt += adjustment.getLineNet();
			dst.addInvoiceLine(line);
			
		}
		
		// Tax amounts
		// ===========
		TaxTotalType ttt = new TaxTotalType();
		TaxAmountType tat = new TaxAmountType();
		taxAmt = UBL21Converter.round(taxAmt, roundingDecimals);
		tat.setValue(BigDecimal.valueOf(taxAmt));
		tat.setCurrencyID(src.getCurrency());
		ttt.setTaxAmount(tat);
		List<TaxTotalType> ttts = new ArrayList<TaxTotalType>();
		ttts.add(ttt);
		dst.setTaxTotal(ttts);
		
		// Add tax subtotals
		for (TaxSubtotalType tsubtotal : tsm.values()) {
			ttt.addTaxSubtotal(tsubtotal);
		}
		
		// Invoice amounts
		
		MonetaryTotalType aMT = new MonetaryTotalType();
		aMT.setPayableAmount(BigDecimal.valueOf(src.getGrandTotal())).setCurrencyID(src.getCurrency());
		aMT.setLineExtensionAmount(BigDecimal.valueOf(lineExtAmt)).setCurrencyID(src.getCurrency());
		aMT.setTaxExclusiveAmount(BigDecimal.valueOf(src.getGrandTotal()-taxAmt)).setCurrencyID(src.getCurrency());
		aMT.setTaxInclusiveAmount(BigDecimal.valueOf(src.getGrandTotal())).setCurrencyID(src.getCurrency());

		dst.setLegalMonetaryTotal(aMT);
		
		
		return dst;
		
	}

	public static InvoiceLineType convertInvoiceLine(InvoiceLine il, String currency, Map<Double,TaxSubtotalType> tsm) {
		
		InvoiceLineType line = new InvoiceLineType();
		line.setID(Integer.toString(il.getLineNo()));
		InvoicedQuantityType iqt = new InvoicedQuantityType();
		iqt.setUnitCode(il.getUOM());
		iqt.setValue(BigDecimal.valueOf(il.getQtyEntered()));
		line.setInvoicedQuantity(iqt);
		line.setLineExtensionAmount(BigDecimal.valueOf(il.getLineNet())).setCurrencyID(currency);
		
		ItemType item = new ItemType();
		line.setItem(item);
		if (il.getName()!=null && il.getName().trim().length()>0) {
			item.setName(il.getName());
			if (il.getDescription()!=null && il.getDescription().trim().length()>0) {
				DescriptionType desc = new DescriptionType();
				desc.setValue(il.getDescription());
				List<DescriptionType> descs = new ArrayList<DescriptionType>();
				descs.add(desc);
				item.setDescription(descs);
			}
		} else {
			if (il.getDescription()!=null && il.getDescription().trim().length()>0) {
				item.setName(il.getDescription());
			}
		}
		// Double check that name is not empty
		if (item.getName()==null || item.getName().toString().trim().length()==0) {
			item.setName("-");
		}

		TaxCategoryType tct = new TaxCategoryType();
		if (il.getTaxPercent()==0) {
			tct.setID("Z");  
		} else {
			tct.setID("S");
		}
		tct.setPercent(BigDecimal.valueOf(il.getTaxPercent()));
		
		TaxSchemeType tst = new TaxSchemeType();
		tst.setID("VAT");
		tct.setTaxScheme(tst);
		item.addClassifiedTaxCategory(tct);
		
		// Lookup taxSubtotal
		TaxSubtotalType taxSubtotal = tsm.get(il.getTaxPercent());
		if (taxSubtotal==null) {
			taxSubtotal = createTaxSubtotal(currency, tct);
			tsm.put(il.getTaxPercent(), taxSubtotal);
		}
		addTaxTotals(taxSubtotal, currency, il.getLineNet(), il.getTaxAmount());
		
		PriceAmountType pat = new PriceAmountType();
		pat.setCurrencyID(currency);
		pat.setValue(BigDecimal.valueOf(il.getPriceActual()));
		PriceType priceType = new PriceType();
		priceType.setPriceAmount(pat);
		line.setPrice(priceType);
		
		return line;
		
	}
	
	public static CreditNoteLineType convertCreditNoteLine(InvoiceLine il, String currency, Map<Double,TaxSubtotalType> tsm) {
		
		CreditNoteLineType line = new CreditNoteLineType();
		line.setID(Integer.toString(il.getLineNo()));
		CreditedQuantityType iqt = new CreditedQuantityType();
		iqt.setUnitCode(il.getUOM());
		iqt.setValue(BigDecimal.valueOf(il.getQtyEntered()));
		line.setCreditedQuantity(iqt);
		line.setLineExtensionAmount(BigDecimal.valueOf(il.getLineNet())).setCurrencyID(currency);
		
		ItemType item = new ItemType();
		line.setItem(item);
		if (il.getName()!=null && il.getName().trim().length()>0) {
			item.setName(il.getName());
			if (il.getDescription()!=null && il.getDescription().trim().length()>0) {
				DescriptionType desc = new DescriptionType();
				desc.setValue(il.getDescription());
				List<DescriptionType> descs = new ArrayList<DescriptionType>();
				descs.add(desc);
				item.setDescription(descs);
			}
		} else {
			if (il.getDescription()!=null && il.getDescription().trim().length()>0) {
				item.setName(il.getDescription());
			}
		}
		// Double check that name is not empty
		if (item.getName()==null || item.getName().toString().trim().length()==0) {
			item.setName("-");
		}
		
		TaxCategoryType tct = new TaxCategoryType();
		if (il.getTaxPercent()==0) {
			tct.setID("Z");  
		} else {
			tct.setID("S");
		}
		tct.setPercent(BigDecimal.valueOf(il.getTaxPercent()));
		
		TaxSchemeType tst = new TaxSchemeType();
		tst.setID("VAT");
		tct.setTaxScheme(tst);
		item.addClassifiedTaxCategory(tct);
		
		// Lookup taxSubtotal
		TaxSubtotalType taxSubtotal = tsm.get(il.getTaxPercent());
		if (taxSubtotal==null) {
			taxSubtotal = createTaxSubtotal(currency, tct);
			tsm.put(il.getTaxPercent(), taxSubtotal);
		}
		addTaxTotals(taxSubtotal, currency, il.getLineNet(), il.getTaxAmount());
		
		PriceAmountType pat = new PriceAmountType();
		pat.setCurrencyID(currency);
		pat.setValue(BigDecimal.valueOf(il.getPriceActual()));
		PriceType priceType = new PriceType();
		priceType.setPriceAmount(pat);
		line.setPrice(priceType);
		
		return line;
		
	}
	
	
	/**
	 * Adds taxableAmount and taxAmount to an existing tst.
	 * 
	 * @param tst
	 * @param currency
	 * @param taxableAmount
	 * @param taxAmount
	 * @return		The same tst but with added amounts. 
	 */
	public static TaxSubtotalType addTaxTotals(TaxSubtotalType tst, String currency, double taxableAmount, double taxAmount) {

		TaxableAmountType tat = tst.getTaxableAmount();
		if (tat==null) {
			tat = new TaxableAmountType();
			tat.setCurrencyID(currency);
			tst.setTaxableAmount(tat);
		}
		TaxAmountType ttt = tst.getTaxAmount();
		if (ttt==null) {
			ttt = new TaxAmountType();
			ttt.setCurrencyID(currency);
			tst.setTaxAmount(ttt);
		}
		
		tat.setValue(tat.getValue().add(BigDecimal.valueOf(taxableAmount)));
		ttt.setValue(ttt.getValue().add(BigDecimal.valueOf(taxAmount)));
		
		return tst;
		
	}

	/**
	 * Creates a new TaxSubtotal
	 * 
	 * @param currency
	 * @param tct
	 * @return
	 */
	public static TaxSubtotalType createTaxSubtotal(String currency, TaxCategoryType tct) {
		
		TaxSubtotalType t = new TaxSubtotalType();
		TaxableAmountType tat = new TaxableAmountType();
		tat.setCurrencyID(currency);
		tat.setValue(BigDecimal.ZERO);
		t.setTaxableAmount(tat);
		
		TaxAmountType ttt = new TaxAmountType();
		ttt.setCurrencyID(currency);
		ttt.setValue(BigDecimal.ZERO);
		t.setTaxAmount(ttt);
		
		t.setTaxCategory(tct);
		
		return t;
		
	}
	
	public static double round(double roundMe, int roundingDecimals) {
		double multiplicator = Math.pow(10, (double)roundingDecimals);
		double result = Math.round(roundMe*multiplicator) / multiplicator;
		return result;
	}

	/**
	 * Writes invoice to string
	 * 
	 * @param src
	 * @return
	 */
	public static String writeToString(InvoiceType src) {

		StringWriter sw = new StringWriter();
		
		UBL21Writer.invoice().write(src, sw);
		
		return sw.toString();
		
	}
	
	/**
	 * Writes Credit Note to string
	 * 
	 * @param src
	 * @return
	 */
	public static String writeCreditNoteToString(CreditNoteType src) {

		StringWriter sw = new StringWriter();
		
		UBL21Writer.creditNote().write(src, sw);
		
		return sw.toString();
		
	}
	
	
	

	/**
	 * Reads invoice from string
	 * 
	 * @param src
	 * @return
	 */
	public static InvoiceType readFromString(String src) {
		
		StringReader sr = new StringReader(src);
		InvoiceType result = UBL21Reader.invoice().read(sr);
		
		return result;
	}

	
	
	@Override
	public String getSystemName() {
		return UBL21Factory.SYSTEMNAME;
	}
	
	@Override
	public String nativeInvoiceToString(InvoiceType src) throws Exception {

		StringWriter sw = new StringWriter();
		UBL21Writer.invoice().write(src, sw);
		
		return sw.toString();
	}

	/**
	 * Reads credit-note from string
	 * @param src
	 * @return
	 */
	public static CreditNoteType readCreditNoteFromString(String src) {
		
		StringReader sr = new StringReader(src);
		CreditNoteType result = UBL21Reader.creditNote().read(sr);
		return result;
		
	}

	// How to create an allowance charge
	/*
	AllowanceChargeType act = new AllowanceChargeType();
	act.setChargeIndicator(adjustment.getLineNet()>0);
	
	AllowanceChargeReasonType actt = new AllowanceChargeReasonType();
	actt.setValue(adjustment.getName());
	act.addAllowanceChargeReason(actt);
	
	oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.AmountType atT = new oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.AmountType();
	atT.setCurrencyID(src.getCurrency());
	atT.setValue(new BigDecimal(Double.toString(adjustment.getLineNet())));
	act.setAmount(atT);
	
	TaxCategoryType tct1 = new TaxCategoryType();
	tct1.setID("Z");
	tct1.setPercent(BigDecimal.ZERO);
	TaxSchemeType tst1 = new TaxSchemeType();
	tst1.setName("VAT");
	tct1.setTaxScheme(tst1);

	TaxExemptionReasonType tert = new TaxExemptionReasonType();
	tert.setValue(adjustment.getName());
	tct1.addTaxExemptionReason(tert); 
	
	act.addTaxCategory(tct1);
	
	// Add tax total
	taxSubtotal = tsm.get(tct1.getPercentValue().doubleValue());
	if (taxSubtotal==null) {
		taxSubtotal = createTaxSubtotal(src.getCurrency(), tct1);
		tsm.put(tct1.getPercentValue().doubleValue(), taxSubtotal);
	}
	addTaxTotals(taxSubtotal, src.getCurrency(), adjustmentAmt, 0);
	
	dst.addAllowanceCharge(act); */

	
}
