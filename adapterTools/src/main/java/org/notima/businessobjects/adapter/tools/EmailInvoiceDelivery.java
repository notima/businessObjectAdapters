package org.notima.businessobjects.adapter.tools;

import java.io.File;
import java.util.Properties;
import java.util.function.Supplier;

import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.ifacebusinessobjects.InvoiceDeliveryBatch;
import org.notima.generic.ifacebusinessobjects.InvoiceDeliveryMethod;
import org.notima.generic.ifacebusinessobjects.KeyManager;

/**
 * Delivers invoices by e-mail, using the e-mail message sender. Only customers
 * that want e-mail invoices get one.
 */
public class EmailInvoiceDelivery implements InvoiceDeliveryMethod {

	public static final String TYPE = "email";

	/** Property: if set, all e-mails go to this address. */
	public static final String RECIPIENT_ADDRESS = "EmailRecipientAddress";
	/** Property: "true" to mark the e-mails as encrypted in the output. */
	public static final String ENCRYPT = "EmailEncrypt";

	private MessageSenderFactory messageSenderFactory;
	private Supplier<KeyManager> keyManagerSupplier;

	/**
	 * @param messageSenderFactory	Provides the e-mail message sender.
	 * @param keyManagerSupplier	Provides the key manager (PGP), if any.
	 */
	public EmailInvoiceDelivery(MessageSenderFactory messageSenderFactory, Supplier<KeyManager> keyManagerSupplier) {
		this.messageSenderFactory = messageSenderFactory;
		this.keyManagerSupplier = keyManagerSupplier;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getDefaultFormat() {
		return "pdf";
	}

	@Override
	public InvoiceDeliveryBatch startBatch(InvoiceList invoices, Properties props) throws Exception {

		MessageSender emailSender = messageSenderFactory!=null ? messageSenderFactory.getMessageSender("email") : null;
		if (emailSender==null) {
			throw new Exception("No e-mail message sender available. Is the notima-email feature installed?");
		}

		return new EmailBatch(emailSender, invoices, props!=null ? props : new Properties());
	}

	private class EmailBatch implements InvoiceDeliveryBatch {

		private MessageSender emailSender;
		private String recipientAddress;
		private boolean encrypt;
		private String subject;
		private String emailBody;
		private int sent = 0;

		EmailBatch(MessageSender emailSender, InvoiceList invoices, Properties props) {
			this.emailSender = emailSender;
			recipientAddress = props.getProperty(RECIPIENT_ADDRESS);
			encrypt = Boolean.parseBoolean(props.getProperty(ENCRYPT));

			String creditorName = invoices.getCreditor()!=null ? invoices.getCreditor().getName() : "";
			subject = "Avi från " + creditorName;
			emailBody = "<p>Se bifogad avi.<br>" + "Kontaktuppgifter till " + creditorName + " i bifogad PDF.</p>" +
			            "<p>Vid frågor om själva utskicket, kontakta oss på 08 776 31 30 eller svara på mailet.</p>" +
					    "<p>Ekonomibolaget Notima AB</p>";
		}

		@Override
		public String deliver(Invoice<?> invoice, File formattedInvoice) throws Exception {

			if (invoice.getBusinessPartner()==null || !invoice.getBusinessPartner().isEmailInvoice()) {
				return null;
			}
			String recipient = (recipientAddress!=null ? recipientAddress : invoice.getBillEmail());
			if (recipient==null) {
				return null;
			}

	        Message message = new Message();
	        Person recipientPerson = new Person();
	        recipientPerson.setEmail(recipient);
	        message.setBody(emailBody);
	        message.setRecipient(recipientPerson);
	        message.setSubject(subject + " " + invoice.getDocumentKey());
	        message.setContentType("text/html;charset=utf-8");
	        message.addAttachment(formattedInvoice);

	        emailSender.send(message, keyManagerSupplier!=null ? keyManagerSupplier.get() : null, false);
	        sent++;

			return "sent to " + recipient + (encrypt ? " using PGP-encryption" : "");
		}

		@Override
		public int complete() {
			return sent;
		}

	}

}
