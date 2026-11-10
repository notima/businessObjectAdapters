package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderLine;
import org.notima.generic.businessobjects.Person;

@Command(scope = "notima", name = "show-canonical-order", description = "Show a specific order in the canonical format")
@Service
public class ShowOrder implements Action {

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	Session sess;

	@Argument(index = 0, name = "adapterName", description ="The adapter name, e.g. Wix", required = true, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String adapterName = "";

	@Argument(index = 1, name = "orgNo", description ="The orgno of the tenant", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo = "";

	@Argument(index = 2, name = "orderNo", description ="The order, as the adapter identifies it (for Wix: order number or order id)", required = true, multiValued = false)
	private String orderNo;

    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode;

    @Option(name = "-s", aliases = { "--summary" }, description = "Show header, customer and lines as tables instead of XML", required = false, multiValued = false)
    private boolean summary;

    @Option(name = "-of", aliases = { "--outfile" }, description = "Write the order as XML to this file", required = false, multiValued = false)
    private String outFile;

	@Override
	public Object execute() throws Exception {

		if (cof.lookupAdapter(adapterName)==null) {
			sess.getConsole().println("Adapter " + adapterName + " not found");
			return null;
		}

		Order<?> order;
		try {
			order = cof.lookupOrder(adapterName, orgNo, countryCode, orderNo);
		} catch (UnsupportedOperationException e) {
			sess.getConsole().println(adapterName + " can't look up orders: " + e.getMessage());
			return null;
		} catch (Exception e) {
			sess.getConsole().println("Could not look up order " + orderNo + ": " + e.getMessage());
			return null;
		}

		if (order==null) {
			sess.getConsole().println("Order " + orderNo + " not found");
			return null;
		}

		// The native order is not part of the canonical format
		order.setNativeOrder(null);

		if (outFile!=null) {
			if (!outFile.endsWith(".xml")) {
				outFile += ".xml";
			}
			JAXB.marshal(order, new File(outFile));
			sess.getConsole().println("Order written to " + outFile);
			return null;
		}

		if (summary) {
			printSummary(order, sess.getConsole());
		} else {
			StringWriter sw = new StringWriter();
			JAXB.marshal(order, sw);
			sess.getConsole().println(sw.toString());
		}

		return null;
	}

	private void printSummary(Order<?> o, PrintStream out) {

		ShellTable header = new ShellTable();
		header.noHeaders();
		header.column("Field");
		header.column("Value");
		header.addRow().addContent("Order key", nvl(o.getOrderKey()));
		header.addRow().addContent("Document key", nvl(o.getDocumentKey()));
		header.addRow().addContent("Your order no", nvl(o.getYourOrderNumber()));
		header.addRow().addContent("Date ordered", date(o.getDateOrdered()));
		header.addRow().addContent("Status", o.getStatus()!=null ? o.getStatus().toString() : "");
		header.addRow().addContent("Currency", nvl(o.getCurrency()));
		header.addRow().addContent("Prices incl VAT", o.isShowPricesIncludingVAT() ? "yes" : "no");
		header.addRow().addContent("Grand total", String.format("%.2f", o.getGrandTotal()));
		header.addRow().addContent("VAT total", String.format("%.2f", o.getVatTotal()));
		if (o.getPaymentTermKey()!=null) header.addRow().addContent("Payment term", o.getPaymentTermKey());
		if (o.getProject()!=null) header.addRow().addContent("Project", o.getProject());
		if (o.getCostCenter()!=null) header.addRow().addContent("Cost center", o.getCostCenter());
		if (o.getShipper()!=null) header.addRow().addContent("Shipper", o.getShipper());

		BusinessPartner<?> bp = o.getBusinessPartner();
		if (bp!=null) {
			header.addRow().addContent("Customer", nvl(bp.getName()) + (bp.isCompany() ? " (company)" : ""));
			if (bp.getIdentityNo()!=null) header.addRow().addContent("Customer no", bp.getIdentityNo());
			if (bp.getTaxId()!=null) header.addRow().addContent("Tax id", bp.getTaxId());
			if (bp.getVatNo()!=null) header.addRow().addContent("VAT no", bp.getVatNo());
			header.addRow().addContent("Address", address(bp.getAddressOfficial()));
			if (bp.getAddressOfficial()!=null) {
				header.addRow().addContent("E-mail", nvl(bp.getAddressOfficial().getEmail()));
				header.addRow().addContent("Phone", nvl(bp.getAddressOfficial().getPhone()));
			}
			if (bp.getContacts()!=null && !bp.getContacts().isEmpty()) {
				Person p = bp.getContacts().get(0);
				header.addRow().addContent("Contact", nvl(p.getName()));
			}
		}
		if (o.getShipLocation()!=null) {
			header.addRow().addContent("Ship to", (o.getShipLocation().getName()!=null ? o.getShipLocation().getName() + ", " : "")
					+ address(o.getShipLocation()));
		}
		header.print(out);
		out.println();

		ShellTable lines = new ShellTable();
		lines.column("#").alignRight();
		lines.column("Product");
		lines.column("Description");
		lines.column("Qty").alignRight();
		lines.column("Price").alignRight();
		lines.column("VAT %").alignRight();
		lines.column("VAT").alignRight();
		lines.column("Net").alignRight();
		for (OrderLine ol : o.getLines()) {
			lines.addRow().addContent(
					ol.getLineNo(),
					nvl(ol.getProductKey()),
					nvl(ol.getName()!=null ? ol.getName() : ol.getDescription()),
					fmt(ol.getQtyEntered()),
					ol.getPriceActual()!=null ? String.format("%.2f", ol.getPriceActual()) : "",
					fmt(ol.getTaxPercent()),
					String.format("%.2f", ol.getTaxAmount()),
					String.format("%.2f", ol.getLineNet()));
		}
		lines.print(out);
		out.println("Prices are " + (o.isShowPricesIncludingVAT() ? "including" : "excluding") + " VAT");
	}

	private static String address(Location l) {
		if (l==null) return "";
		StringBuilder sb = new StringBuilder();
		append(sb, l.getAddress1());
		append(sb, l.getAddress2());
		append(sb, (nvl(l.getPostal()) + " " + nvl(l.getCity())).trim());
		append(sb, l.getCountryCode());
		return sb.toString();
	}

	private static void append(StringBuilder sb, String s) {
		if (s==null || s.trim().isEmpty()) return;
		if (sb.length()>0) sb.append(", ");
		sb.append(s.trim());
	}

	private static String fmt(double d) {
		return d==Math.rint(d) ? String.valueOf((long)d) : String.valueOf(d);
	}

	private static String date(Date d) {
		return d!=null ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(d) : "";
	}

	private static String nvl(String s) {
		return s!=null ? s : "";
	}

}
