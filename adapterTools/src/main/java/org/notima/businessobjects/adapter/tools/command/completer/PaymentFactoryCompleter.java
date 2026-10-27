package org.notima.businessobjects.adapter.tools.command.completer;

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;

/**
 * Completes the system names of the payment factories (payment sources such as P27 or Adyen).
 */
@Service
public class PaymentFactoryCompleter implements Completer {

	@Reference(optional = true)
	private List<PaymentFactory> paymentFactories;

	@Override
	public int complete(Session session, CommandLine commandLine, List<String> candidates) {

		StringsCompleter delegate = new StringsCompleter();
		if (paymentFactories!=null) {
			for (PaymentFactory pf : paymentFactories) {
				delegate.getStrings().add(pf.getSystemName());
			}
		}
		return delegate.complete(session, commandLine, candidates);

	}

}
