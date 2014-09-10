/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
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
