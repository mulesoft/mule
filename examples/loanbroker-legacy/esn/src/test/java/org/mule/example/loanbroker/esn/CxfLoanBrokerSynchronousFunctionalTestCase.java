/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.esn;

import org.mule.example.loanbroker.tests.AbstractLoanBrokerTestCase;

public class CxfLoanBrokerSynchronousFunctionalTestCase extends AbstractLoanBrokerTestCase
{
    
    @Override
    public void testSingleLoanRequest() throws Exception
    {
        super.testSingleLoanRequest();
    }

    @Override
    protected String getConfigResources()
    {
        return "loan-broker-sync-config.xml, loan-broker-cxf-endpoints-config.xml";
    }
}
