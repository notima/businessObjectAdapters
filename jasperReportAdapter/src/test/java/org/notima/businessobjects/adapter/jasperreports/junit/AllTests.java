package org.notima.businessobjects.adapter.jasperreports.junit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Tests run by the build. The other tests in this package need example data that isn't
 * part of the repository.
 */
@RunWith(Suite.class)
@SuiteClasses({ TestInvoiceLanguage.class })
public class AllTests {

}
