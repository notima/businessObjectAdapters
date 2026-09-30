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

### Thresholds

A channel can have thresholds that stop it from being processed when a report file contains too many unmatched payments. A payment is unmatched if no invoice is found for it, if the invoice found is already paid, or if the invoice couldn't be looked up (ie the destination system is unreachable).

Before a report file is processed, `process-payment-channel` matches its payments against the destination system (read only) and checks the thresholds. If a threshold is exceeded, the channel stops at that file: that file and all later files are left unprocessed in the source directory and "reconciled until" is not changed. The reason is printed after the processing report, for example:

	Channel stopped at 2026-09-25.json: 4 unmatched payments, limit 3
	The file and any later files were not processed. Use --force to process anyway.

There are three thresholds. Each is optional and applies to one report file (all currencies in the file together, except for the amount):

| Threshold | Option | Example |
|---|---|---|
| Max number of unmatched payments | `--max-unmatched` | `3` |
| Max share of unmatched payments, in percent | `--max-unmatched-percent` | `10` |
| Max unmatched amount per currency | `--max-unmatched-amount` | `5000` or `"5000,{500:EUR}"` |

The amount uses the same syntax as the general ledger accounts. A plain value applies to all currencies that aren't listed separately, so `"5000,{500:EUR}"` means 500 for EUR and 5000 for every other currency. If only separate currencies are listed (ie `"{500:EUR}"`), unmatched payments in any other currency exceed the threshold. Refunds count by their absolute amount.

#### Setting thresholds

Thresholds are set on the channel (and saved with it) using `modify-payment-channel`. The channel can be given by ID or description.

	modify-payment-channel --max-unmatched 3 [channelId]
	modify-payment-channel --max-unmatched-percent 10 --max-unmatched-amount "5000,{500:EUR}" [channelId]

Use `none` to remove a threshold:

	modify-payment-channel --max-unmatched none [channelId]

All values are validated before anything is changed on the channel.

#### Viewing thresholds

	show-payment-channel-status [channelId]

The rows `Max unmatched`, `Max unmatched %` and `Max unmatched amt` show the thresholds. `-` means no threshold.

#### Running with thresholds

- `process-payment-channel [channelId]` checks the thresholds of each report file before processing it.
- `process-payment-channel --dry-run [channelId]` checks the thresholds and reports where the channel would stop, but continues showing the payments and vouchers of all files.
- `process-payment-channel --force [channelId]` ignores the thresholds for this run, ie after checking the report.

A channel without thresholds is processed as before, without the extra lookup.
