/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.esn;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.service.Service;
import org.mule.example.loanbroker.tests.AbstractLoanBrokerTestCase;
import org.mule.model.seda.SedaModel;

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
        Object objModel = muleContext.getRegistry().lookupModel("loan-broker");
        assertNotNull(objModel);
        assertTrue(objModel instanceof SedaModel);
        SedaModel model = (SedaModel)objModel;
        assertComponent(model, "TheLoanBroker");
        assertComponent(model, "TheCreditAgencyService");
        assertComponent(model, "TheLenderService");
        assertComponent(model, "TheBankGateway");
    }

    protected void assertComponent(SedaModel model, String serviceName)
    {
        Service service = muleContext.getRegistry().lookupService(serviceName);
        assertNotNull(serviceName + " missing", service);
    }
}
