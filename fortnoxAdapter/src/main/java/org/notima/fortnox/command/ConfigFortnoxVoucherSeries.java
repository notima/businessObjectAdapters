package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.FinancialYearSubset;
import org.notima.api.fortnox.entities3.VoucherSeries;
import org.notima.api.fortnox.entities3.VoucherSeriesSubset;
import org.notima.fortnox.command.completer.FortnoxTenantCompleter;

@Command(scope = "fortnox", name = "config-fortnox-voucher-series", description = "Configure voucher series.")
@Service
public class ConfigFortnoxVoucherSeries extends FortnoxCommand implements Action {
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(FortnoxTenantCompleter.class)	
	private String orgNo = "";

	@Argument(index = 1, name = "code", description ="The voucher series to configure. If it doesn't exist, it's created", required = true, multiValued = false)
	private String code = "";

	@Option(name = "--yearId", description = "Config voucher for specific yearId", required = false, multiValued = false)
	private Integer yearId;
	
	@Option(name = "--description", description = "Description of the voucher series", required = false, multiValued = false)
	private String description;
	
	@Option(name = "--manual", description = "If the voucher series should be allowed to be created manually (defaults to true).", required = false, multiValued = false)
	private Boolean manual;
	
	
	@Override
	public Object execute() throws Exception {
		
		try {
		
			FortnoxClient3 fc = getFortnoxClient(orgNo);
			if (fc == null) {
				sess.getConsole().println("Can't get client for " + orgNo);
				return null;
			}

			if (yearId==null) {
				FinancialYearSubset fys = fc.getFinancialYear(null);
				if (fys==null) {
					sess.getConsole().println("No financial year defined.");
					return null;
				}
				yearId = fys.getId();
			}
			
			boolean create = false;
			VoucherSeries vs = null;
			VoucherSeriesSubset vss = fc.getVoucherSeries(code, yearId);
			if (vss!=null) {
				vs = new VoucherSeries(vss);
			}
			
			if (vs==null) {
				
				String reply = sess.readLine("Voucher series [" + code + "] doesn't exist. Do you want to create it? (y/n) ", null);
				if (reply.equalsIgnoreCase("y")) {
					
					vs = new VoucherSeries();
					vs.setCode(code);
					vs.setManual(Boolean.TRUE);
					create = true;
					
				} else {
					sess.getConsole().println("Operation cancelled.");
				}
				
			}
			
				
			if (description!=null) {
				vs.setDescription(description);
			}
			if (manual!=null) {
				vs.setManual(manual);
			}
			
			fc.setVoucherSeries(vs, yearId);
			sess.getConsole().println("Voucher series " + code + " " + (create ? "created" : "updated") + ".");
			
		} catch (FortnoxException fe) {
			sess.getConsole().println(fe.toString());
		}
		
		return null;
	}
	
}
