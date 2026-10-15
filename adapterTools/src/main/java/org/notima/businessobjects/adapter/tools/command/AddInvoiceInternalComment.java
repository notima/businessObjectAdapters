package org.notima.businessobjects.adapter.tools.command;

import java.util.Arrays;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.InvoiceInternalCommenter;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "add-invoice-internal-comment", description = "Appends an internal comment to one or more sales invoices in the adapter's underlying system")
@Service
public class AddInvoiceInternalComment extends AbstractAction {

	@Reference
	private CanonicalObjectFactory cof;

    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;

	@Argument(index = 0, name = "adapterName", description ="The adapter name", required = true, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String adapterName = "";

	@Argument(index = 1, name = "orgNo", description ="The orgno of the tenant", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo = "";

	@Argument(index = 2, name = "comment", description ="The comment to append, ie \"NotimaInvoiced 261015\"", required = true, multiValued = false)
	private String comment;

	@Argument(index = 3, name = "invoiceNumbers", description ="The invoice(s) to append the comment to", required = true, multiValued = true)
	private String[] invoiceNumbers;

	@SuppressWarnings("rawtypes")
	@Override
	protected Object onExecute() throws Exception {

		BusinessObjectFactory adapter = cof.lookupAdapter(adapterName);
		if (adapter==null) {
			sess.getConsole().println("No adapter " + adapterName);
			return null;
		}
		adapter.setTenant(orgNo, countryCode);

		try {
			new InvoiceInternalCommenter(adapter, comment, sess.getConsole()).appendTo(Arrays.asList(invoiceNumbers));
		} catch (UnsupportedOperationException uoe) {
			sess.getConsole().println(uoe.getMessage());
		}

		return null;
	}

}
