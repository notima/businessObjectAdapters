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

## Tenant information

Tenant information is extra information about a tenant that isn't part of the tenant's adapter, ie where to write files and how the tenant gets paid. It's stored separately from the adapters, so a tenant in an adapter that can't store this kind of information (ie Adempiere) can still have it.

### Where it's stored

The tenant information is stored by a `TenantInformationFactory`. The jsonAdapter provides one (install the `notima-json` feature), which stores the information in `jsonAdapter/tenantInformation.json` in Karaf's home directory. The file can be changed with `tenantInformationFile` in `etc/jsonAdapterProperties.cfg`.

If more than one adapter provides a `TenantInformationFactory`, choose which one to use with `tenantInformationAdapter` (the adapter's system name) in `etc/AdapterTools.cfg`:

	tenantInformationAdapter = Json

Without the setting, the first one (by system name) is used and a warning is logged. The setting is read when the adapterTools bundle starts.

### Identifying the tenant

A tenant is identified by org number and country code. If the country code isn't given with `-co`, the `defaultCountryCode` in `etc/AdapterTools.cfg` is used (`SE` by default). Information stored with one country code isn't found with another.

### Setting tenant information

	set-tenant-info [orgNo] [attribute] [value]
	set-tenant-info -co SE 556745-6941 defaultOutputDirectory /home/user/karaf-output/notima

The entry is created if it doesn't exist. One attribute is set per command (tab completes the attribute names):

| Attribute | Description |
|---|---|
| `legalName` | The tenant's name, used in file names created by `read-invoices` |
| `taxId` | The org number |
| `countryCode` | The country code |
| `defaultOutputDirectory` | Where files are written when no file is given, ie by `read-invoices` and `show-canonical-invoice -of default` |
| `reportDirectory` | Where reports are written, ie payment channel match reports. If not set, `defaultOutputDirectory` is used |
| `remitToAccount` | The account invoices are paid to, ie a bankgiro number |
| `remitToAccountType` | The type of `remitToAccount`, ie `BG` (bankgiro) or `PG` (plusgiro) |
| `remitToIBAN` | IBAN invoices are paid to |
| `remitToBIC` | BIC for the IBAN |

Note that changing `taxId` or `countryCode` changes how the entry is identified.

### Showing tenant information

	show-tenant-info [orgNo]
	show-tenant-info -co SE 556745-6941

Shows the stored information. If nothing is stored for the tenant, the registered adapters are searched for a tenant with the org number instead.

To see the output and report directory of all tenants in the adapters:

	list-tenants --with-info [adapter]

`list-tenants` lists the tenants in the adapters. A tenant only stored in the tenant information isn't listed.

### Where it's used

- `read-invoices` writes to `defaultOutputDirectory` if no file is given, named `[legalName or orgNo]-[yyyyMMdd].xml`.
- `show-canonical-invoice -of default` writes to `defaultOutputDirectory`, named `ar-invoice-[invoiceNo].xml` for sales invoices and `ap-invoice-[invoiceNo].xml` for vendor invoices.
- `read-invoices` sets the payment information (`remitTo...`) on the creditor of sales invoices. If `remitToAccount` or `remitToIBAN` is set in the tenant information, the tenant information's payment information overrides whatever the adapter supplied. All four `remitTo...` values are taken from the tenant information, also the ones not set, so they're never mixed with the adapter's. If neither has any, a warning is printed.
- Payment channel reports are written to `reportDirectory`, see [Report directory per tenant](#report-directory-per-tenant).

## Printing invoices

`print-invoices` formats the invoices in a file created by `read-invoices` (xml) or a json invoice list, one file per invoice:

	print-invoices [-format pdf|peppol] [-od outputDirectory] [--delivery email|ekopost] [file]
	print-invoices -format peppol /home/user/karaf-output/notima/556745-6941-20261001.xml

Each file is named after the invoice's document key (with `-email` added for invoices sent by e-mail), and is written to `-od` or else the directory of the input file. The path of each file is printed.

| Format | Output | Provided by feature |
|---|---|---|
| `pdf` (default without delivery) | PDF | `notima-jasperreport` |
| `peppol` | E-invoice, UBL 2.1 Peppol BIS Billing 3.0 (`.xml`) | `notima-ubl` |

For `peppol`:
- Invoices with a negative total are written as a UBL CreditNote, with positive amounts.
- The seller is the invoice's sender. If the invoice has none, the creditor of the invoice list is used.
- The payment means (bankgiro, plusgiro or IBAN/BIC) come from the creditor's payment information. `read-invoices` puts it on the creditor, from the adapter or from the tenant information (`remitTo...`, see [Setting tenant information](#setting-tenant-information)). The payment reference is the invoice's OCR, or the invoice number if there is no OCR. Without payment information the e-invoice has no payment means and a warning is logged.

### Delivering invoices

With `--delivery`, each formatted invoice is also delivered to the customer. Without `-format`, the delivery method's format is used.

	print-invoices --delivery ekopost /home/user/karaf-output/notima/556745-6941-20261001.xml
	print-invoices -s -a test@example.com [file]

| Delivery | Format | Delivers | Provided by |
|---|---|---|---|
| `email` (also `-s`) | `pdf` | Sends each invoice right away to the customer's billing e-mail. Only customers that want e-mail invoices get one. `-a` sends all e-mails to that address instead (ie for testing). | adapterTools, using the e-mail message sender (`notima-email`) |
| `ekopost` | `peppol` | Queues all invoices in the file and sends them to Ekopost (Peppol) in one batch at the end. Uses the `Ekopost` configuration. | `ekopost-api` (notima-integration) |

For each invoice the result is printed (ie `sent to a@b.se`, `queued for Ekopost` or `not delivered by email`), and at the end the number of invoices delivered. An unknown delivery method gives an error listing the available ones.

A delivery method is an `InvoiceDeliveryMethod` service (`getType()`, `getDefaultFormat()`, `startBatch(...)`). The interfaces `InvoiceDeliveryMethod` and `InvoiceDeliveryBatch` are in businessobjects (`org.notima.generic.ifacebusinessobjects`), so new methods (ie Kivra) can be added in their own bundle without depending on adapterTools or changing `print-invoices`.

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

### Report directory per tenant

Reports are written to the tenant's report directory, which is stored in the tenant information:

	set-tenant-info [orgNo] reportDirectory /path/to/reports/tenant
	show-tenant-info [orgNo]

If `reportDirectory` isn't set, the tenant's `defaultOutputDirectory` is used. The tenant is identified by org number and country code (default from the AdapterTools settings, or `-co`). Channels stored without country code belong to the tenant with the same org number.

`process-payment-channel -format xls` writes its report to the tenant's report directory, unless an output file is given with `-of`. If the tenant has no report directory, the file is written relative to Karaf's working directory as before.

### Match report task

`PaymentChannelMatchReportTask` matches the pending report files of a tenant's **active** channels against their destination systems, the same way as `process-payment-channel --match-only`, and writes one report per channel to the tenant's report directory. Nothing is booked, no files are moved and the channels aren't changed.

- The task is created for one tenant (org number). Create one task per tenant.
- One report per channel with pending payments, named like the `-format` output of `process-payment-channel`, ie `ZaverSE_2026-09-28.json_260929.xls`. A run with the same pending files overwrites the previous report.
- Channels without pending payments, without source directory or with a missing adapter are skipped (and logged). A failing channel doesn't stop the others.
- If the tenant has no report directory, the task fails with a message telling how to set it. The directory is created if it doesn't exist.
- The format is `xls` by default (requires the excelAdapter).
- The task is locked per tenant, so the same tenant's task doesn't run twice at the same time. The locks are files in `${karaf.data}/task-locks`. A lock left by a Karaf process that no longer runs (ie after a restart) is ignored and removed.

Show and remove task locks:

	list-task-locks
	unlock-task payment-channel-match-report-556677-8899

The task can be run manually from the Karaf shell. It uses the same task lock as a scheduled run and prints its progress:

	run-match-report [orgNo]
	run-match-report -co SE --report-dir /tmp/reports -format xls [orgNo]
	run-match-report --until 2026-09-30 [orgNo]

	ZaverSE: 3 payments, 1 matched, 2 unmatched (66.67 %), unmatched amount SEK 325.00
	ZaverSE: thresholds: max unmatched 40 %, max unmatched amount 5000,{500:EUR}
	ZaverSE: processing would stop at 2026-09-28.json: 50.00 % unmatched payments (1 of 2), limit 40.00 %
	ZaverSE: report written to /path/to/reports/tenant/ZaverSE_2026-09-28.json_260929.xls
	1 active channels, 1 reports written to /path/to/reports/tenant

For each channel the task states the matching result (unmatched means no invoice found, the invoice is already paid or couldn't be looked up), the channel's thresholds, and whether `process-payment-channel` would stop at one of the pending files (thresholds are checked per report file). From the shell this is printed to the console; when scheduled or run from a route it's written to the log.

Properties that can be set on the bean (the command's options set the same):

| Property | Default |
|---|---|
| `countryCode` | The default country code in the AdapterTools settings |
| `reportDirectory` | The tenant's report directory (overrides it if set) |
| `format` | `xls` |
| `untilDate` | None, all pending files are matched. With `--until yyyy-MM-dd`, only files dated until (and including) the date are matched, the same rule as `process-payment-channel --untildate` |

The task is created in a Blueprint file in Karaf's `deploy` directory and can be triggered either by a Camel route or by the Karaf scheduler.

Triggered by a Camel route, ie once a day:

	<bean id="matchReportTask" class="org.notima.businessobjects.adapter.tools.task.PaymentChannelMatchReportTask">
		<argument value="555555-5555"/>
	</bean>

	<camelContext id="payment-channel-match-report" xmlns="http://camel.apache.org/schema/blueprint">
		<route id="payment-channel-match-report">
			<from uri="timer://matchReport?delay=60000&amp;period=86400000"/>
			<to uri="bean:matchReportTask?method=run"/>
		</route>
	</camelContext>

Triggered by the Karaf scheduler (feature `scheduler`), ie every weekday at 07:00:

	<bean id="matchReportTask" class="org.notima.businessobjects.adapter.tools.task.PaymentChannelMatchReportTask">
		<argument value="555555-5555"/>
	</bean>

	<service ref="matchReportTask" interface="java.lang.Runnable">
		<service-properties>
			<entry key="scheduler.name" value="payment-channel-match-report-555555-5555"/>
			<entry key="scheduler.expression" value="0 0 7 ? * MON-FRI"/>
			<entry key="scheduler.concurrent" value="false"/>
		</service-properties>
	</service>

The format can be changed with `<property name="format" value="..."/>` on the bean, for any format a report formatter supports. Note that the excelAdapter always writes the xls format, also when asked for xlsx.
