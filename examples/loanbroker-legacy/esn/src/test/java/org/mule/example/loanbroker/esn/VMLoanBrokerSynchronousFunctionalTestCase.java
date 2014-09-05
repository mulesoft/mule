/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.esn;

import static org.junit.Assert.assertNotNull;
import org.mule.construct.Flow;
import org.mule.example.loanbroker.tests.AbstractLoanBrokerTestCase;

import org.junit.Test;

public class VMLoanBrokerSynchronousFunctionalTestCase extends AbstractLoanBrokerTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "loan-broker-sync-config.xml, loan-broker-vm-endpoints-config.xml";
    }

    @Override
    protected int getNumberOfRequests()
    {
        return 1000;
    }

    @Test
    public void testBasicParsing()
    {
        assertComponent("TheLoanBroker");
        assertComponent("TheCreditAgencyService");
        assertComponent("TheLenderService");
        assertComponent("TheBankGateway");
    }

    protected void assertComponent(String flowName)
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
        assertNotNull(flowName + " missing", flow);
    }
}
