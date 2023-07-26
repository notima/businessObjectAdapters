# businessObjectAdapters
Contains adapters to different ERP / invoice / order sources / destinations

## Prerequisites

To compile this project in a SNAPSHOT-version, you'll first have to checkout and compile below projects

https://github.com/notima/businessobjects

https://github.com/notima/fortnox4j

https://github.com/notima/svea-pmt-admin-api

https://github.com/notima/siefilelib

https://github.com/sveawebpay/webpay-common

https://github.com/sveawebpay/webpayadminreports

https://github.com/notima/swish4j

If the project is a release version, you'll only have to checkout and compile the project itself.

## Installation in Karaf (fortnox adapter)

	repo-add mvn:org.notima.generic.businessobjects.adapter/adapterTools/LATEST/xml/features
	
	feature:install notima-fortnox4j
	
## Structure

The adapters contained in this repository are meant to be OSGI-plugins in a Karaf-environment. They can also be used stand-alone.

Every adapter exposes its services in the Activator class.

To be consequent about where loggers are placed, put them in the *Adapter class(es) of the module.