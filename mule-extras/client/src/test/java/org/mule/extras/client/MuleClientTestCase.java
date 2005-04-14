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
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleClientTestCase extends AbstractMuleTestCase
{
    public void setUp() throws Exception
    {
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("test-client-mule-config.xml");
    }

    public void testClientSendDirect() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleManager.getConfiguration().setSynchronous(true);

        UMOMessage message = client.sendDirect("TestReceiverUMO", null, "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Received: Test Client Send message", message.getPayload());
    }

    public void testClientDispatchDirect() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleManager.getConfiguration().setSynchronous(true);

        client.dispatchDirect("TestReceiverUMO", "Test Client dispatch message", null);
    }

    public void testClientSend() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration().setSynchronousReceive(true);

        UMOMessage message = client.send(getDispatchUrl(), "Test Client Send message", null);
        assertNotNull(message);
        assertEquals("Received: Test Client Send message", message.getPayload());
    }

    public void testClientMultiSend() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration().setSynchronousReceive(true);
        
         for(int i = 0;i < 100;i++) {
            UMOMessage message = client.send(getDispatchUrl(), "Test Client Send message " + i, null);
            assertNotNull(message);
            assertEquals("Received: Test Client Send message " + i, message.getPayload());
         }
    }

    public void testClientMultidispatch() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleManager.getConfiguration().setSynchronous(false);

        int i = 0;
        //to init
        client.dispatch(getDispatchUrl(), "Test Client Send message " + i, null);
        long start = System.currentTimeMillis();
         for(i = 0;i < 100;i++) {
            client.dispatch(getDispatchUrl(), "Test Client Send message " + i, null);
         }
        long time = System.currentTimeMillis() - start;
        System.out.println(i + " took " + time + "ms to process");
        Thread.sleep(1000);
    }
   

    public String getDispatchUrl()
    {
        return "vm://test.queue";
    }
}
