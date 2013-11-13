/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.esn;

import org.mule.example.loanbroker.tests.AbstractAsynchronousLoanBrokerTestCase;

public class CxfLoanBrokerAsynchronousFunctionalTestCase extends AbstractAsynchronousLoanBrokerTestCase
{
    @Override
    public void testLotsOfLoanRequests() throws Exception
    {
        super.testLotsOfLoanRequests();
    }

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] { "loan-broker-async-config.xml" , "loan-broker-cxf-endpoints-config.xml" };
    }

    @Override
    protected int getNumberOfRequests()
    {
        return 10;
    }

    @Override
    protected int getWarmUpMessages()
    {
        // MULE-3016
        return 1;
    }
}
