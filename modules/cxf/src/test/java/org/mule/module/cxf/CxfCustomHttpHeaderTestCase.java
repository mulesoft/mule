/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class CxfCustomHttpHeaderTestCase extends AbstractServiceAndFlowTestCase implements EndpointMessageNotificationListener
{
    protected String endpointAddress = null;
    private List<MuleMessage> notificationMsgList = new ArrayList<MuleMessage>();
    private CountDownLatch latch = null;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public CxfCustomHttpHeaderTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "headers-conf-service.xml"},
            {ConfigVariant.FLOW, "headers-conf-flow.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        latch = new CountDownLatch(2);
        muleContext.registerListener(this);
        endpointAddress = ((InboundEndpoint) muleContext.getRegistry()
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

        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, "alan");
        props.put(MuleProperties.MULE_METHOD_PROPERTY, "onReceive");
        props.put(myProperty, myProperty);

        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("cxf:" + endpointAddress, payload, props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("Test String Received", reply.getPayloadAsString());

        // make sure all notifications have trickled in
        Thread.sleep(3000);

        // make sure we received the notifications on cxf
        assertEquals(2, notificationMsgList.size());

        // MULE_USER should be allowed in
        assertEquals("alan", notificationMsgList.get(0).getOutboundProperty(MuleProperties.MULE_USER_PROPERTY));

        // mule properties should be removed
        assertNull(notificationMsgList.get(0).getOutboundProperty(MuleProperties.MULE_IGNORE_METHOD_PROPERTY));

        // custom properties should be allowed in
        assertEquals(myProperty, notificationMsgList.get(0).getOutboundProperty(myProperty));
    }

    @Override
    public void onNotification(ServerNotification notification)
    {
        if (notification instanceof EndpointMessageNotification)
        {
            String uri = ((EndpointMessageNotification) notification).getEndpoint();
            if (endpointAddress.equals(uri))
            {
                notificationMsgList.add((MuleMessage) notification.getSource());
                latch.countDown();
            }
        }
        else
        {
            fail("invalid notification: " + notification);
        }
    }
}
