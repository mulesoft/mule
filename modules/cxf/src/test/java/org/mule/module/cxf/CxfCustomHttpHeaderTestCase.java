/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class CxfCustomHttpHeaderTestCase extends FunctionalTestCase implements EndpointMessageNotificationListener
{
    protected String endpointAddress = null;
    private MuleMessage notificationMsg = null;
    private CountDownLatch latch = null;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "headers-conf.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        latch = new CountDownLatch(1);
        muleContext.registerListener(this);
        MuleClient client = new MuleClient(muleContext);
        endpointAddress = ((InboundEndpoint) client.getMuleContext().getRegistry()
                        .lookupObject("cxfInbound")).getAddress() + "?method=onReceive";
    }

    @Override
    protected void doTearDown() throws Exception
    {
        muleContext.unregisterListener(this);
    }

    @Test
    public void testCxf() throws Exception
    {
        Object payload = new Object[]{"Test String"};
        String myProperty = "myProperty";

        HashMap<String, String> props = new HashMap<String, String>();
        props.put(MuleProperties.MULE_USER_PROPERTY, "alan");
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "onReceive");
        props.put(myProperty, myProperty);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("cxf:" + endpointAddress, payload, props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("Test String Received", reply.getPayloadAsString());

        // make sure all notifications have trickled in
        Thread.sleep(3000);

        // make sure we received a notification on cxf
        assertNotNull(notificationMsg);

        // MULE_USER should be allowed in
        assertEquals("alan", notificationMsg.getOutboundProperty(MuleProperties.MULE_USER_PROPERTY));

        // mule properties should be removed
        assertNull(notificationMsg.getOutboundProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY));

        // custom properties should be allowed in
        assertEquals(myProperty, notificationMsg.getOutboundProperty(myProperty));
    }

    public void onNotification(ServerNotification notification)
    {
        if (notification instanceof EndpointMessageNotification)
        {
            String uri = ((EndpointMessageNotification) notification).getEndpoint();
            if (endpointAddress.equals(uri))
            {
                notificationMsg = (MuleMessage) notification.getSource();
                latch.countDown();
            }
        }
        else
        {
            fail("invalid notification: " + notification);
        }
    }
}
