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

public class XFireLoanBrokerSynchronousFunctionalTestCase extends AxisLoanBrokerSynchronousFunctionalTestCase
{

    public XFireLoanBrokerSynchronousFunctionalTestCase()
    {
        super();
    }
    
    // @Override
    protected String getConfigResources()
    {
        return "loan-broker-xfire-sync-test-config.xml";
    }

    // @Override
    protected int getNumberOfRequests()
    {
        return 10;
    }

}
