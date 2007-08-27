/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.esn;

import org.mule.examples.loanbroker.tests.AbstractLoanBrokerTestCase;
import org.mule.impl.model.seda.SedaModel;
import org.mule.umo.UMOComponent;


public class VMLoanBrokerSynchronousFunctionalTestCase extends AbstractLoanBrokerTestCase
{
    // @Override
    protected String getConfigResources()
    {
        return "loan-broker-sync-config.xml, loan-broker-vm-endpoints-config.xml";
    }

    // @Override
    protected int getNumberOfRequests()
    {
        return 1000;
    }

    public void testBasicParsing()
    {
        Object objModel = managementContext.getRegistry().lookupModel("loan-broker");
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
        UMOComponent component = model.getComponent(name);
        assertNotNull(name + " missing", component);
    }

}
