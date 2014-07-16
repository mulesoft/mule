/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEventContext;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class QuartzEventGeneratorTestCase extends AbstractServiceAndFlowTestCase
{

    private static final String PAYLOAD = "TRIGGER STRING";

    private final List<String> receivedPayloads = new ArrayList<String>();

    public QuartzEventGeneratorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "quartz-event-generator-service.xml"},
            {ConfigVariant.FLOW, "quartz-event-generator-flow.xml"}});
    }

    @Test
    public void testEventGeneratorPayload() throws Exception
    {
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("quartzService");
        assertNotNull(component);
        CountdownCallback callback = new Callback(1, receivedPayloads);
        component.setEventCallback(callback);

        // wait for incoming messages
        assertTrue(callback.await(60000));
        assertTrue(receivedPayloads.size() > 0);
        assertEquals(PAYLOAD, receivedPayloads.get(0));
    }

    private static class Callback extends CountdownCallback
    {
        private List<String> messageList;

        public Callback(int messagesExpected, List<String> payloadStore)
        {
            super(messagesExpected);
            messageList = payloadStore;
        }

        @Override
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            synchronized (this)
            {
                String payloadString = context.getMessage().getPayloadAsString();
                messageList.add(payloadString);
            }

            super.eventReceived(context, component);
        }
    }

}
