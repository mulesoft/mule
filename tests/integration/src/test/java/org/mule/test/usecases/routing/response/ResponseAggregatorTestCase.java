/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.routing.response;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.routing.requestreply.AbstractAsyncRequestReplyRequester;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.SensingNullMessageProcessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.util.store.SimpleMemoryObjectStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResponseAggregatorTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/usecases/routing/response/response-router-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/usecases/routing/response/response-router-flow.xml"}});
    }

    public ResponseAggregatorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSyncResponse() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("http://localhost:28081", "request", null);
        assertNotNull(message);
        assertEquals("Received: request", new String(message.getPayloadAsBytes()));
    }

    @Test
    public void testResponseEventsCleanedUp() throws Exception
    {
        RelaxedAsyncReplyMP mp = new RelaxedAsyncReplyMP();

        try
        {
            MuleEvent event = getTestEvent("message1");
            final MuleMessage message = event.getMessage();
            final String id = message.getUniqueId();
            message.setCorrelationId(id);
            message.setCorrelationGroupSize(1);

            SensingNullMessageProcessor listener = getSensingNullMessageProcessor();
            mp.setListener(listener);
            mp.setReplySource(listener.getMessageSource());

            mp.process(event);

            Map<String, MuleEvent> responseEvents = mp.getResponseEvents();
            assertTrue("Response events should be cleaned up.", responseEvents.isEmpty());
        }
        finally
        {
            mp.stop();
        }
    }

    /**
     * This class opens up the access to responseEvents map for testing
     */
    private static final class RelaxedAsyncReplyMP extends AbstractAsyncRequestReplyRequester
    {
        private RelaxedAsyncReplyMP() throws MuleException
        {
            store = new SimpleMemoryObjectStore();
            name = "asyncReply";
            start();
        }

        public Map<String, MuleEvent> getResponseEvents()
        {
            return responseEvents;
        }
    }
}
