/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.routing.requestreply.AbstractAsyncRequestReplyRequester;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class ResponseAggregatorTestCase extends AbstractIntegrationTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/response/response-router-flow.xml";
    }

    @Test
    public void testSyncResponse() throws Exception
    {
        MuleClient client = muleContext.getClient();
        final HttpRequestOptions httpRequestOptions = newOptions().method(POST.name()).build();
        MuleMessage message = client.send(format("http://localhost:%s", port.getNumber()), MuleMessage.builder().payload("request").build(), httpRequestOptions);
        assertNotNull(message);
        assertThat(new String(getPayloadAsBytes(message)), is("Received: request"));
    }

    @Test
    public void testResponseEventsCleanedUp() throws Exception
    {
        RelaxedAsyncReplyMP mp = new RelaxedAsyncReplyMP();

        try
        {
            MuleEvent event = getTestEvent("message1");
            final MuleMessage message = MuleMessage.builder(event.getMessage())
                                                   .correlationId(event.getMessage().getUniqueId())
                                                   .correlationGroupSize(1)
                                                   .build();
            event.setMessage(message);

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
            store = new SimpleMemoryObjectStore<>();
            name = "asyncReply";
            start();
        }

        public Map<String, MuleEvent> getResponseEvents()
        {
            return responseEvents;
        }
    }
}
