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
