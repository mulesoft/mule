/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.inbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IdempotentRouterWithFilterTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/inbound/idempotent-router-with-filter.xml";
    }

    @Test
    public void testWithValidData()
    {
        /*
         * This test will pass a message containing a String to the Mule server and
         * verifies that it gets received.
         */
        MuleClient myClient;
        DefaultMuleMessage myMessage = new DefaultMuleMessage("Mule is the best!", muleContext);
        MuleMessage response = null;

        try
        {
            myClient = new MuleClient(muleContext);
            myClient.dispatch("vm://FromTestCase", myMessage);
            response = myClient.request("vm://ToTestCase", 5000);
        }
        catch (MuleException e)
        {
            fail(e.getDetailedMessage());
        }

        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("Mule is the best!", response.getPayload());
    }

    @Test
    public void testWithInvalidData()
    {
        /*
         * This test will pass a message containing an Object to the Mule server and
         * verifies that it does not get received.
         */
        MuleClient myClient;
        DefaultMuleMessage myMessage = new DefaultMuleMessage(new Object(), muleContext);
        MuleMessage response = null;

        try
        {
            myClient = new MuleClient(muleContext);
            myClient.dispatch("vm://FromTestCase", myMessage);
            response = myClient.request("vm://ToTestCase", 5000);
        }
        catch (MuleException e)
        {
            fail(e.getDetailedMessage());
        }

        assertNull(response);
    }

}
