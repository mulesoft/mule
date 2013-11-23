/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MuleClientListenerTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "org/mule/test/integration/client/mule-client-listener-config-flow.xml"}});
    }

    public MuleClientListenerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    public void doTestRegisterListener(String component, String endpoint, boolean canSendWithoutReceiver)
        throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        try
        {
            client.send(endpoint, "Test Client Send message", null);
        }
        catch (DispatchException e)
        {
            if (!canSendWithoutReceiver)
            {
                assertTrue(e.getCause() instanceof NoReceiverForEndpointException);
            }
        }

        Object c = muleContext.getRegistry().lookupObject(component);

        MuleMessage message = client.send(endpoint, "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Received: Test Client Send message", message.getPayloadAsString());

        // The SpringRegistry is read-only so we can't unregister the service!
        // muleContext.getRegistry().unregisterComponent("vmComponent");
        ((Stoppable) c).stop();

        try
        {
            client.send(endpoint, "Test Client Send message", null);
        }
        catch (DispatchException e)
        {
            if (!canSendWithoutReceiver)
            {
                assertTrue(e.getCause() instanceof NoReceiverForEndpointException);
            }
        }
    }

    @Test
    public void testRegisterListenerVm() throws Exception
    {
        doTestRegisterListener("vmComponent", "vm://test.queue", false);
    }

    @Test
    public void testRegisterListenerTcp() throws Exception
    {
        doTestRegisterListener("tcpComponent", "tcp://localhost:56324", true);
    }
}
