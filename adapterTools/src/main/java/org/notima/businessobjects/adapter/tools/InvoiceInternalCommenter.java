package org.notima.businessobjects.adapter.tools;

import java.io.PrintStream;
import java.util.Collection;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Appends an internal comment to invoices in an adapter's underlying system, ie to mark
 * the invoices that have been sent or reminded.
 */
public class InvoiceInternalCommenter {

	@SuppressWarnings("rawtypes")
	private final BusinessObjectFactory adapter;
	private final String comment;
	private final PrintStream out;

	/**
	 * @param adapter	The adapter, with its tenant set.
	 * @param comment	The comment to append.
	 * @param out		Where the result for each invoice is printed.
	 */
	@SuppressWarnings("rawtypes")
	public InvoiceInternalCommenter(BusinessObjectFactory adapter, String comment, PrintStream out) {
		this.adapter = adapter;
		this.comment = comment;
		this.out = out;
	}

	/**
	 * Creates a commenter for invoices read from a file, ie by print-invoices or print-dunning-run.
	 * 
	 * @param cof			Used to look up the adapter.
	 * @param adapterName	The adapter the invoices were read from.
	 * @param orgNo			The tenant's org no, or null to use the creditor's.
	 * @param creditor		The creditor of the invoices in the file.
	 * @param comment		The comment to append.
	 * @param out			Where the result for each invoice is printed.
	 * @throws Exception	If the adapter or tenant can't be found.
	 */
	@SuppressWarnings("rawtypes")
	public static InvoiceInternalCommenter forFile(CanonicalObjectFactory cof, String adapterName, String orgNo,
			BusinessPartner<?> creditor, String comment, PrintStream out) throws Exception {
		
		if (adapterName==null) {
			throw new Exception("--internal-comment requires --adapter");
		}
		BusinessObjectFactory adapter = cof.lookupAdapter(adapterName);
		if (adapter==null) {
			throw new Exception("No adapter " + adapterName);
		}
		String countryCode = creditor!=null ? creditor.getCountryCode() : null;
		if (orgNo==null && creditor!=null && creditor.getTaxId()!=null && creditor.getTaxId().trim().length()>0) {
			orgNo = creditor.getTaxId().trim();
		}
		if (orgNo==null) {
			throw new Exception("The file has no creditor org no. Use --org-no to give the tenant for --internal-comment");
		}
		adapter.setTenant(orgNo, countryCode);
		return new InvoiceInternalCommenter(adapter, comment, out);
	}
	
	/**
	 * Appends the comment to the given invoices. A failure on one invoice is printed and
	 * doesn't stop the others.
	 *
	 * @param invoiceKeys	The invoices to append the comment to.
	 * @return	The number of invoices updated.
	 * @throws UnsupportedOperationException	If the adapter doesn't support internal comments.
	 */
	public int appendTo(Collection<String> invoiceKeys) {

		int count = 0;
		for (String invoiceKey : invoiceKeys) {
			try {
				String result = adapter.appendInvoiceInternalComment(invoiceKey, comment);
				if (result==null) {
					out.println(invoiceKey + ": invoice not found");
				} else {
					out.println(invoiceKey + ": " + result.replace('\n', '|'));
					count++;
				}
			} catch (UnsupportedOperationException uoe) {
				throw uoe;
			} catch (Exception e) {
				out.println(invoiceKey + ": " + e.getMessage());
			}
		}
		out.println("Internal comment added to " + count + " of " + invoiceKeys.size() + " invoice(s)");
		return count;
	}

}
