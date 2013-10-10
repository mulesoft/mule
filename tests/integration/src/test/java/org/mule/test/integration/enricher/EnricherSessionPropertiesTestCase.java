/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.enricher;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;

public class EnricherSessionPropertiesTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/enricher/enricher-session-properties.xml";
    }

    @Test
    public void testCallingFlowUsingFlowRefInsideEnricher() throws Exception
    {
        System.setProperty("mule.enricher.propagateSession", "true");
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in", "some message", null, 3000);
        assertThat(response, IsNull.<Object> notNullValue());
        assertThat(response.getPayloadAsString(), is("some message"));
        assertThat(response.getExceptionPayload(), IsNull.<Object> nullValue());
    }

    public static class Spy implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            Object enrichedContent = event.getMessage().getInvocationProperty("enrichedContent");
            assertThat((String) enrichedContent, is("some message received by subflow"));
            assertThat(event.getSession().<String> getProperty("subFlowSessionProperty"), is("someValue"));
            assertThat(event.getMessage().<String> getSessionProperty("subFlowSessionProperty"),
                is("someValue"));
            assertThat(event.getSession().<String> getProperty("mainFlowSessionProperty"), is("someValue"));
            return event;
        }
    }
}
