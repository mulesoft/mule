/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.exception.AbstractExceptionListener;
import org.mule.exception.ChoiceMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class ReferenceExceptionStrategyTestCase extends FunctionalTestCase
{

    public static final int TIMEOUT = 5000;
    public static final String JSON_RESPONSE = "{\"errorMessage\":\"error processing news\",\"userId\":15,\"title\":\"News title\"}";
    public static final String JSON_REQUEST = "{\"userId\":\"15\"}";

    @Override
    protected String getConfigResources()
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
