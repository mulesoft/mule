/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.loanbroker;



/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GlueLoanBrokerSynchronousFunctionalTestCase extends AxisLoanBrokerSynchronousFunctionalTestCase {

    protected String getConfigResources() {
        return "loan-broker-glue-sync-test-config.xml";
    }
}
