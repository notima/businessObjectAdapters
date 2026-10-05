package org.notima.businessobjects.adapter.tools;

/**
 * Interface that formats an invoice.
 * <p>
 * Moved to {@link org.notima.generic.ifacebusinessobjects.InvoiceFormatter} so that
 * applications can use invoice formatter plugins without depending on adapterTools.
 * This type remains so that existing OSGi service registrations and lookups keep working.
 *
 * @author Daniel Tamm
 * @deprecated Implement and look up {@link org.notima.generic.ifacebusinessobjects.InvoiceFormatter}.
 */
@Deprecated
public interface InvoiceFormatter extends org.notima.generic.ifacebusinessobjects.InvoiceFormatter {

}
