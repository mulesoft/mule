/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
