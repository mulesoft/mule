/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.exception.AbstractExceptionListener;
import org.mule.exception.ChoiceMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.Test;

public class ReferenceExceptionStrategyTestCase extends FunctionalTestCase
{
    public static final int TIMEOUT = 5000;
    public static final String JSON_RESPONSE = "{\"errorMessage\":\"error processing news\",\"userId\":15,\"title\":\"News title\"}";
    public static final String JSON_REQUEST = "{\"userId\":\"15\"}";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/reference-flow-exception-strategy.xml";
    }

    @Test
    public void testFlowUsingGlobalExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in", JSON_REQUEST, null, 5000);
        assertThat(response, IsNull.<Object>notNullValue());
        // compare the structure and values but not the attributes' order
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualJsonNode = mapper.readTree(response.getPayloadAsString());
        JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
        assertThat(actualJsonNode, is(expectedJsonNode));
    }

    @Test
    public void testFlowUsingConfiguredExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in2", JSON_REQUEST, null, 5000);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat((NullPayload) response.getPayload(), is(NullPayload.getInstance()));
        assertThat(response.getExceptionPayload(), IsNull.<Object>notNullValue());
    }

    @Test
    public void testTwoFlowsReferencingSameExceptionStrategyGetDifferentInstances()
    {
        MessagingExceptionHandler firstExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("otherFlowWithSameReferencedExceptionStrategy").getExceptionListener();
        MessagingExceptionHandler secondExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("referenceExceptionStrategyFlow").getExceptionListener();
        assertThat(firstExceptionStrategy, IsNot.not(secondExceptionStrategy));
    }

    @Test
    public void testTwoFlowsReferencingDifferentExceptionStrategy()
    {
        MessagingExceptionHandler firstExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("otherFlowWithSameReferencedExceptionStrategy").getExceptionListener();
        MessagingExceptionHandler secondExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("anotherFlowUsingDifferentExceptionStrategy").getExceptionListener();
        assertThat(firstExceptionStrategy, IsNot.not(secondExceptionStrategy));
        assertThat(((AbstractExceptionListener)firstExceptionStrategy).getMessageProcessors().size(), is(2));
        assertThat(secondExceptionStrategy, instanceOf(ChoiceMessagingExceptionStrategy.class));
    }

}
