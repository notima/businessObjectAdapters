## Fortnox commands

All available commands can be listed by typing 'fortnox' and press tab in the karaf console

	karaf@root()> fortnox

### Client management commands

### Invoice management commands

#### modify-fortnox-invoice

```
DESCRIPTION
        fortnox:modify-fortnox-invoice

	Modify a specific invoice

SYNTAX
        fortnox:modify-fortnox-invoice [options] orgNo invoiceNo property [value] 

ARGUMENTS
        orgNo
                The orgno of the client
                (required)
        invoiceNo
                The invoice no
                (required)
        property
                The property to modify
                (required)
        value
                The new value for the property

OPTIONS
        --help
                Display this help message
        --no-confirm
                Don't confirm anything. Default is to confirm
        --all
                Modify all invoices according to filter (unbooked, TODO-date)
```

The currently available properties to modify are

* warehouseReady
* fixCommentLines
* dueDate
* invoiceDate
* paymentTerm
* copyPaymentTerm - Copies payment term from customer to invoice.
* extref1
* extref2
* comment
* copyCustomerName - Copies customer name from customer to invoice.
