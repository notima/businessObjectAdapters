package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceWriterOptions;
import org.notima.generic.businessobjects.OrderList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.util.LocalDateUtils;
import org.notima.util.json.JsonUtil;

/**
 * Reads canonical orders from a file (as written by show-canonical-order -of) and writes them
 * to the destination adapter.
 */
@Command(scope = "notima", name = "write-orders", description = "Reads orders from a canonical XML/JSON-file and writes them to the destination adapter")
@Service
public class WriteOrders extends AbstractAction {

	@Reference
	private CanonicalObjectFactory cof;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "--create-customers", description = "Create customers not found in the destination system", required = false, multiValued = false)
	private boolean createCustomers;

	@Option(name = "--order-date", description = "Order date (yyyy-mm-dd) for orders without a date", required = false, multiValued = false)
	private String orderDateStr;

	@Option(name = "--create-limit", description = "Maximum number of orders to create", required = false, multiValued = false)
	private Integer createLimit;

	@Argument(index = 0, name = "adapterName", description = "The destination adapter name, e.g. Fortnox", required = true, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String adapterName = "";

	@Argument(index = 1, name = "orgNo", description = "The org number of the tenant to write to", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo;

	@Argument(index = 2, name = "orderFile", description = "The canonical order file (xml or json). Either a single order or an order list.", required = true, multiValued = false)
	@Completion(FileCompleter.class)
	private String orderFile = "";

	@Override
	protected Object onExecute() throws Exception {

		BusinessObjectFactory<?,?,?,?,?,?> adapter = cof.lookupAdapter(adapterName);
		if (adapter==null) {
			sess.getConsole().println("Adapter " + adapterName + " not found");
			return null;
		}

		File file = new File(orderFile);
		if (!file.isFile()) {
			sess.getConsole().println("File " + orderFile + " not found");
			return null;
		}

		List<Order<?>> orders = readOrders(file);
		if (orders.isEmpty()) {
			sess.getConsole().println("No orders found in " + orderFile);
			return null;
		}

		OrderInvoiceWriterOptions opts = new OrderInvoiceWriterOptions();
		opts.setCreateBusinessPartner(createCustomers);
		opts.setUpdateExisting(false);
		opts.setCreateLimit(createLimit!=null ? createLimit : 0);
		if (orderDateStr!=null) {
			opts.setOrderDate(LocalDateUtils.asLocalDate(dfmt.parse(orderDateStr)));
		}

		adapter.setTenant(orgNo, countryCode);
		if (adapter.getCurrentTenant()==null) {
			sess.getConsole().println("Tenant " + orgNo + " not found in " + adapterName);
			return null;
		}

		OrderInvoiceOperationResult result;
		try {
			result = adapter.writeOrders(orders, opts);
		} catch (UnsupportedOperationException e) {
			sess.getConsole().println(e.getMessage());
			return null;
		}

		printResult(orders, result);
		return null;
	}

	private List<Order<?>> readOrders(File file) throws Exception {

		List<Order<?>> orders = new ArrayList<Order<?>>();
		String name = file.getName().toLowerCase();

		if (name.endsWith(".json")) {
			try (FileReader fr = new FileReader(file)) {
				OrderList list = JsonUtil.buildGson().fromJson(fr, OrderList.class);
				if (list!=null && list.getOrderList()!=null) orders.addAll(list.getOrderList());
			}
			return orders;
		}

		// XML: a single <order> (show-canonical-order -of) or an <orderList>
		if (isOrderList(file)) {
			OrderList list = JAXB.unmarshal(file, OrderList.class);
			if (list!=null && list.getOrderList()!=null) orders.addAll(list.getOrderList());
		} else {
			Order<?> order = JAXB.unmarshal(file, Order.class);
			if (order!=null) orders.add(order);
		}
		return orders;
	}

	private boolean isOrderList(File file) throws Exception {
		javax.xml.stream.XMLStreamReader r = javax.xml.stream.XMLInputFactory.newInstance()
				.createXMLStreamReader(new java.io.FileInputStream(file));
		try {
			while (r.hasNext()) {
				if (r.next()==javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
					return "orderList".equals(r.getLocalName());
				}
			}
			return false;
		} finally {
			r.close();
		}
	}

	private void printResult(List<Order<?>> orders, OrderInvoiceOperationResult result) {

		int written = 0;
		for (Order<?> o : orders) {
			String key = o.getOrderKey()!=null ? o.getOrderKey() : "(no order key)";
			if (o.getDocumentKey()!=null && !o.getDocumentKey().trim().isEmpty()) {
				written++;
				sess.getConsole().println("Order " + key + " -> " + adapterName + " order " + o.getDocumentKey());
			} else {
				sess.getConsole().println("Order " + key + " NOT written (customer missing"
						+ (createCustomers ? "" : ", use --create-customers") + ", create limit or error - see log)");
			}
		}
		int created = result!=null ? result.getCreateCount() : 0;
		sess.getConsole().println(orders.size() + " order(s) read, " + created + " created, "
				+ (written - created) + " already existed, " + (orders.size() - written) + " not written.");
	}

}
