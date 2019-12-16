package com.svea.businessobjects;

import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Person;

/**
 * Helper class to convert invoice and order lines to Svea Format.
 * 
 * Helps convert address to Svea Format.
 * 
 * @author daniel.tamm
 *
 */
public class SveaUtility {

	// Determines how exact rounding is calculated.
	public static final int ROUNDING_DECIMALS = 3;

    public static String getCountryCode(Location location) {
		String countryCode = location.getCountryCode();

		// Country specific settings
		if ("FI".equals(countryCode) || "AX".equals(countryCode) || "AA".equals(countryCode)) {
			return "FI";
		} else {
			return countryCode;
		}
		
    }
    
	/**
	 * Remove all non digit characters
	 *
	 * @param cleanUp
	 * @return
	 */
	public static String toDigitsOnly(String cleanUp) {
		if (cleanUp==null) return("");
		StringBuffer buf = new StringBuffer();
		char c;
		for (int i=0; i<cleanUp.length();i++) {
			c = cleanUp.charAt(i);
			if (c>='0' && c<='9') {
				buf.append(c);
			}
		}
		return(buf.toString());
	}
    
	public static XMLGregorianCalendar getXMLDate(java.util.Date d) throws DatatypeConfigurationException {
		
		 GregorianCalendar cal = new GregorianCalendar();
	      XMLGregorianCalendar date2 = null;
	      if (d!=null) {
	              cal.setTime(d);
	              date2 = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
	      }
	      
		return date2;
	}
	
	/**
	 * This method sets information in the info object using the passed parameters.
	 * It doesn't set the address map of the info object it just returns the address map.
	 * The reason it doesn't set the address map is because it doesn't know whether the
	 * address is billing or shipping address.
	 * 
	 * It's up to the caller to determine what kind of address it is and set it correctly
	 * on the info object.
	 * 
	 * @param customer
	 * @param user
	 * @param location
	 * @param info
	 * @return
	 * @throws Exception
	 */
	public static Map<String,String> setFromLocation(BusinessPartner customer, Person user, Location location) throws Exception {

		String countryCode = location.getCountryCode();
		boolean isCompany = false;
		
		// Default country code to SE if missing
		if (countryCode==null)
			countryCode = "se";

		String countryStr = countryCode.toLowerCase();
		
		StringBuffer street = new StringBuffer();
		if (location.getAddress1()!=null && location.getAddress1().trim().length()>0) {
			street.append(location.getAddress1());
		}
		if (location.getAddress2()!=null && location.getAddress2().trim().length()>0) {
			if (street.length()>0) street.append("\n");
			street.append(location.getAddress2());
		}
		if (location.getAddress3()!=null && location.getAddress3().trim().length()>0) {
			if (street.length()>0) street.append("\n");
			street.append(location.getAddress3());
		}
		if (location.getAddress4()!=null && location.getAddress4().trim().length()>0) {
			if (street.length()>0) street.append("\n");
			street.append(location.getAddress4());
		}
		
		String fname = null;
		String lname = null;
		if (!customer.isCompany()) {
			fname = user.getFirstName();
			lname = user.getLastName();
			if (fname==null || fname.trim().length()==0 || lname==null || lname.trim().length()==0) {
				// Split into first and last name
				fname = customer.getName();
				int firstSpace = fname.lastIndexOf(" ");
				if (firstSpace>0 && firstSpace<(fname.length()-1)) {
					lname = fname.substring(firstSpace+1);
					fname = fname.substring(0, firstSpace);
				}
			}
		} else {
			isCompany = true;
			// Legal entity
			fname = customer.getName();
			lname = "";
		}
		
		// Create Address
		Map<String,String> adr = (Map<String,String>)mk_address(fname, lname, street.toString(), location.getPostal(), location.getCity(), countryStr, isCompany);
		
		return(adr);
	}
	
    /*************************************************************************
     * API: mk_address
     *************************************************************************/
    public static Map<String,String> mk_address(String fname, String lname, String street,
            String postno, String city, String country, boolean isCompany) {
        Map<String, String> address = new Hashtable<String, String>();
        address.put("fname", fname);
        address.put("lname", lname);
        address.put("street", street);
        address.put("zip", postno);
        address.put("city", city);
        address.put("country", country);
        if (isCompany) {
        	address.put("isCompany", "true");
        }
        return address;
    }
	
	
}
