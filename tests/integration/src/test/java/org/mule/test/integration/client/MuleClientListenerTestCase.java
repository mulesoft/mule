/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.client;


import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.NoReceiverForEndpointException;


public class MuleClientListenerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/mule-client-listener-config.xml";
    }

    public void doTestRegisterListener(String component, String endpoint, boolean canSendWithoutReceiver) throws Exception
    {
        MuleClient client = new MuleClient();

        if (!canSendWithoutReceiver)
        {
            try
            {
                client.send(endpoint, "Test Client Send message", null);
                fail("There is no receiver for this endpointUri");
            }
            catch (Exception e)
            {
                assertTrue(e.getCause() instanceof NoReceiverForEndpointException);
            }
        }
        
        UMOComponent c = managementContext.getRegistry().lookupComponent(component);
        c.start();

        UMOMessage message = client.send(endpoint, "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Received: Test Client Send message", message.getPayloadAsString());

        // The SpringRegistry is read-only so we can't unregister the component!
        //managementContext.getRegistry().unregisterComponent("vmComponent");
        c.stop();

        if (!canSendWithoutReceiver)
        {
            try
            {
                message = client.send(endpoint, "Test Client Send message", null);
                fail("There is no receiver for this endpointUri");
            }
            catch (Exception e)
            {
                assertTrue(e.getCause() instanceof NoReceiverForEndpointException);
            }
        }
    }

    public void testRegisterListenerVm() throws Exception
    {
        doTestRegisterListener("vmComponent", "vm://test.queue", false);
    }

    public void testRegisterListenerTcp() throws Exception
    {
        doTestRegisterListener("tcpComponent", "tcp://localhost:56324", true);
    }

}
