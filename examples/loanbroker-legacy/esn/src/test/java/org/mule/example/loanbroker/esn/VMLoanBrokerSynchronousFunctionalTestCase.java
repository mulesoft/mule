/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.esn;

import org.mule.api.service.Service;
import org.mule.example.loanbroker.tests.AbstractLoanBrokerTestCase;
import org.mule.model.seda.SedaModel;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VMLoanBrokerSynchronousFunctionalTestCase extends AbstractLoanBrokerTestCase
{

    @Override
    protected String getConfigResources()
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

    protected void assertComponent(SedaModel model, String name)
    {
        Service service = muleContext.getRegistry().lookupService(name);
        assertNotNull(name + " missing", service);
    }

}
