package org.notima.businessobjects.adapter.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.ifacebusinessobjects.InvoiceDeliveryBatch;
import org.notima.generic.ifacebusinessobjects.KeyManager;

class TestEmailInvoiceDelivery {

	private List<Message> sentMessages = new ArrayList<Message>();

	private MessageSender fakeSender = new MessageSender() {
		@Override
		public String getType() {
			return "email";
		}
		@Override
		public void send(Message message, KeyManager keyManager, boolean attachSenderPublicKey) {
			sentMessages.add(message);
		}
	};

	private EmailInvoiceDelivery delivery = new EmailInvoiceDelivery(type -> "email".equals(type) ? fakeSender : null, () -> null);

	private InvoiceList invoiceList() {
		InvoiceList list = new InvoiceList();
		BusinessPartner<Object> creditor = new BusinessPartner<Object>();
		creditor.setName("Creditor AB");
		list.setCreditor(creditor);
		return list;
	}

	private Invoice<Object> invoice(String no, boolean emailInvoice, String email) {
		Invoice<Object> invoice = new Invoice<Object>();
		invoice.setDocumentKey(no);
		BusinessPartner<Object> customer = new BusinessPartner<Object>();
		customer.setEmailInvoice(emailInvoice);
		invoice.setBusinessPartner(customer);
		invoice.setBillBpartner(customer);
		Person person = new Person();
		person.setEmail(email);
		invoice.setBillPerson(person);
		return invoice;
	}

	@Test
	void onlyEmailCustomersGetAnEmail() throws Exception {
		InvoiceDeliveryBatch batch = delivery.startBatch(invoiceList(), new Properties());
		File pdf = new File("1001.pdf");

		assertEquals("sent to kund@example.com", batch.deliver(invoice("1001", true, "kund@example.com"), pdf));
		assertNull(batch.deliver(invoice("1002", false, "annan@example.com"), new File("1002.pdf")));
		assertNull(batch.deliver(invoice("1003", true, null), new File("1003.pdf")));
		assertEquals(1, batch.complete());

		assertEquals(1, sentMessages.size());
		Message message = sentMessages.get(0);
		assertEquals("kund@example.com", message.getRecipient().getEmail());
		assertEquals("Avi från Creditor AB 1001", message.getSubject());
		assertEquals(pdf, message.getAttachemnts().get(0));
		assertTrue(message.getBody().contains("Creditor AB"));
	}

	@Test
	void recipientAddressOverridesCustomersAddress() throws Exception {
		Properties props = new Properties();
		props.setProperty(EmailInvoiceDelivery.RECIPIENT_ADDRESS, "test@example.com");
		InvoiceDeliveryBatch batch = delivery.startBatch(invoiceList(), props);

		assertEquals("sent to test@example.com", batch.deliver(invoice("1001", true, "kund@example.com"), new File("1001.pdf")));
		assertEquals("test@example.com", sentMessages.get(0).getRecipient().getEmail());
	}

	@Test
	void noEmailSender() {
		EmailInvoiceDelivery withoutSender = new EmailInvoiceDelivery(type -> null, () -> null);
		assertThrows(Exception.class, () -> withoutSender.startBatch(invoiceList(), new Properties()));
	}

}
