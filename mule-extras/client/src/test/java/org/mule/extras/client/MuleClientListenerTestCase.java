/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.client;

import org.mule.MuleManager;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.NoReceiverForEndpointException;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleClientListenerTestCase extends NamedTestCase
{
    public void doTestRegisterListener(String urlString, boolean canSendWithoutReceiver) throws Exception
    {
        MuleClient client = new MuleClient();
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration().setSynchronousReceive(true);

        if (!canSendWithoutReceiver) {
            try {
                client.send(urlString, "Test Client Send message", null);
                fail("There is no receiver for this endpointUri");
            } catch (Exception e) {
                assertTrue(e.getCause() instanceof NoReceiverForEndpointException);
            }
        }

        TestReceiver receiver = new TestReceiver();
        MuleEndpointURI url = new MuleEndpointURI(urlString);
        String name = "myComponent";
        client.registerComponent(receiver, name, url);
        assertTrue(MuleManager.getInstance().getModel().isComponentRegistered(name));

        UMOMessage message = client.send(urlString, "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Received: Test Client Send message", message.getPayloadAsString());
        client.unregisterComponent(name);

        assertTrue(!MuleManager.getInstance().getModel().isComponentRegistered(name));

        if (!canSendWithoutReceiver) {
            try {
                message = client.send(urlString, "Test Client Send message", null);
                fail("There is no receiver for this endpointUri");
            } catch (Exception e) {
                assertTrue(e.getCause() instanceof NoReceiverForEndpointException);
            }
        }
    }

    public void testRegisterListenerVm() throws Exception
    {
        doTestRegisterListener("vm://localhost/test.queue", false);
    }

    public void testRegisterListenerTcp() throws Exception
    {
        doTestRegisterListener("tcp://localhost:56324", true);
    }

    protected void tearDown() throws Exception
    {
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
    }

    protected void setUp() throws Exception
    {
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
    }
}
