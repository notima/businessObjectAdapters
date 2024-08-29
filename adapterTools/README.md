# Adapter Tools

This module contains commands and classes that work on the other modules in this library.

The logic is that the command specify which system/adapter (module) to call.

To see what adapters are enabled:

	list-adapters

List all tenants for a specific adapter

	list-tenants [adapter]

To list all customers for a specific adapter

	list-business-partners [adapter] [orgNo of tenant]

Copy tenant information from one adapter to another

	copy-tenant [srcAdapter] [dstAdapter] [orgNo of tenant]
	

	
## Payment batches

Payment batches are a concept for reconciling payments. A payment batch here is a canonical format to represent a collection of payments with associated fees and payment transfer.

### Payment factory

A payment factory is a producer of payment batches. It takes the proprietary format of the factory implementation and formats it in a format readable by a payment batch processor.

The currently available payment processors can be listed using

	list-adapters
	
To use a payment factory one could use this command

	show-payment-batch [adapter] [source]
	
The format of the source is defined by the specific adapter used. Use the option --raw to see the actual payment batch in json.

### Payment batch processor

A payment batch processor is a consumer of payment batches. That means that it's responsible for applying the information in the payment batch to a target system and tenant.

List all payment batch processors

	list-payment-batch-processors
	
## Processing payments

Processing of payments involves both reading a payment batch from a payment factory and sending it to a payment batch processor.

One way of doing this is to use the command process-payment-batch

	process-payment-batch --options [destination] [paymentFactoryAdapter] [srcForPaymentFactoryAdapter]
	
- The destination system specifies to which payment batch processor the batch should be sent.
- The combined payment factory adapter and source creates a payment batch. The batch itself contains information about which tenant in the destination adapter they payments should be sent to.

## Payment channels

Payment channels are a way of defining payments to be processed.

	list-payment-batch-channels [orgNo]

	