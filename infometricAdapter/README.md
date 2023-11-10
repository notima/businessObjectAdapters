# infometricAdapter

Converts Infometric measurement files to business objects invoices.

Depends on Apache Commons CSV 1.6

## Installation

	feature:install notima-infometric
	
Set the base directory for where incoming billing files.

	 property-set -p InfometricProperties infometricBaseDirectory "/opt/infometric"
	 
A restart of the bundle is needed after setting above property.

## Usage examples

###Add client

	add-infometric-client --orgName "Test AB" 555555-5555 "subpath"
	
Subpath is a path found below infometricBaseDirectory where the billing files specific for this client is stored.

### Create a billing file

Create a billing file in XML based on the specific client's billing files

	read-invoices Infometric 555555-5555 /output/file.xml
	


