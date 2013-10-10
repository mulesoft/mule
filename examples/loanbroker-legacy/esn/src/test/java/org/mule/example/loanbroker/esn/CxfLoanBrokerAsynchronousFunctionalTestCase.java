/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
    {
        return "loan-broker-async-config.xml, loan-broker-cxf-endpoints-config.xml";
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
