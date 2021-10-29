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

If the project is a release version, you'll only have to checkout and compile the sveawebpay projects.

## Installation in Karaf

	repo-add mvn:org.notima.generic.businessobjects.adapter/adapterTools/LATEST/xml/features
	
	feature:install notima-fortnox4j