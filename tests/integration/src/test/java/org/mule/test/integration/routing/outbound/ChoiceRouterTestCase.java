/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.filter.Filter;
import org.mule.construct.Flow;
import org.mule.routing.ChoiceRouter;
import org.mule.routing.MessageProcessorFilterPair;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.routing.filters.RegExFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ChoiceRouterTestCase extends FunctionalTestCase
{
    private static final String WITH_DEFAULT_ROUTE_CHANNEL = "vm://with-default-route.in";
    private static final String WITHOUT_DEFAULT_ROUTE_CHANNEL = "vm://without-default-route.in";

    public ChoiceRouterTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/choice-router-test.xml";
    }

    /**
     * Check that the choice router was assembled correctly by the BDP machinery
     */
    @Test
    public void filterReferenceShouldCreateFilterWithRegexFilterAndOutboundEndpointChain()
    {
        ChoiceRouter router = findChoiceRouterInFlow("without-default-route");
        MessageProcessorFilterPair pair = router.getConditionalMessageProcessors().get(0);
        assertIsRegExFilterWithPattern(pair.getFilter(), "apple");
        assertMessageChainIsOutboundEndpoint(pair.getMessageProcessor(), "vm://fruit-channel.in");
    }

    /**
     * Check that the choice router was assembled correctly by the BDP machinery
     */
    @Test
    public void embeddedFilterShouldCreatePairWithFilterAndOtherConfiguredMPsAsChain()
    {
        ChoiceRouter router = findChoiceRouterInFlow("with-default-route");

        MessageProcessorFilterPair firstPair = router.getConditionalMessageProcessors().get(0);
        assertIsRegExFilterWithPattern(firstPair.getFilter(), "apple");
        assertMessageChainIsOutboundEndpoint(firstPair.getMessageProcessor(), "vm://fruit-channel.in");

        MessageProcessorFilterPair secondPair = router.getConditionalMessageProcessors().get(1);
        assertIsRegExFilterWithPattern(secondPair.getFilter(), "turnip");
        assertMessageChainIsOutboundEndpoint(secondPair.getMessageProcessor(), "vm://veggie-channel.in");

        MessageProcessorFilterPair thirdPair = router.getConditionalMessageProcessors().get(2);
        assertIsExpressionFilterWithExpressionAndEvaluator(thirdPair.getFilter(), ".*berry", "regex");
        assertMessageChainIsOutboundEndpoint(thirdPair.getMessageProcessor(), "vm://fruit-channel.in");
    }

    @Test
    public void sendToInvalidRouteWithoutDefaultRouteShouldThrowException() throws Exception
    {
        MuleMessage result = muleContext.getClient().send(WITHOUT_DEFAULT_ROUTE_CHANNEL, "bad", null);
        assertNotNull(result);
        assertNotNull("should have got a MuleException", result.getExceptionPayload());
        assertNotNull(result.getExceptionPayload().getException() instanceof MuleException);
        assertNotNull(result.getExceptionPayload().getRootException() instanceof RoutePathNotFoundException);
    }

    @Test
    public void sendToValidRouteShouldReturnValidResult() throws Exception
    {
        MuleMessage result = muleContext.getClient().send(WITHOUT_DEFAULT_ROUTE_CHANNEL, "apple", null);
        assertEquals("apple:fruit:fruit", result.getPayloadAsString());
    }

    @Test
    public void sendToAppleRouteShouldHitFruitService() throws Exception
    {
        MuleMessage result = muleContext.getClient().send(WITH_DEFAULT_ROUTE_CHANNEL, "apple", null);
        assertEquals("apple:fruit:fruit", result.getPayloadAsString());
    }

    @Test
    public void sendToTurnipRouteShouldHitVeggieService() throws Exception
    {
        MuleMessage result = muleContext.getClient().send(WITH_DEFAULT_ROUTE_CHANNEL, "turnip", null);
        assertEquals("turnip:veggie:veggie", result.getPayloadAsString());
    }

    @Test
    public void sendToBlueberryRouteShouldHitFruitService() throws Exception
    {
        MuleMessage result = muleContext.getClient().send(WITH_DEFAULT_ROUTE_CHANNEL, "blueberry", null);
        assertEquals("blueberry:fruit:fruit", result.getPayloadAsString());
    }

    @Test
    public void sendToInvalidRouteShouldHitDefaultRoute() throws Exception
    {
        MuleMessage result = muleContext.getClient().send(WITH_DEFAULT_ROUTE_CHANNEL, "car", null);
        assertEquals("car:default:default", result.getPayloadAsString());
    }

    private ChoiceRouter findChoiceRouterInFlow(String flowName)
    {
        Flow flow = lookupFlow(flowName);
        return getChoiceRouter(flow);
    }

    private Flow lookupFlow(String flowConstructName)
    {
        FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct(flowConstructName);
        assertNotNull(flow);
        assertTrue(flow instanceof Flow);
        return (Flow) flow;
    }

    private ChoiceRouter getChoiceRouter(Flow flowConstruct)
    {
        MessageProcessor routerMessageProcessor = flowConstruct.getMessageProcessors().get(0);
        assertTrue(routerMessageProcessor instanceof ChoiceRouter);
        return (ChoiceRouter) routerMessageProcessor;
    }

    private void assertIsRegExFilterWithPattern(Filter filter, String pattern)
    {
        assertTrue(filter instanceof RegExFilter);

        RegExFilter regExFilter = (RegExFilter) filter;
        assertEquals(regExFilter.getPattern(), pattern);
    }

    private void assertIsExpressionFilterWithExpressionAndEvaluator(Filter filter, String expression, String evaluator)
    {
        assertTrue(filter instanceof ExpressionFilter);

        ExpressionFilter expressionFilter = (ExpressionFilter) filter;
        assertEquals(expression, expressionFilter.getExpression());
        assertEquals(evaluator, expressionFilter.getEvaluator());
    }

    private void assertMessageChainIsOutboundEndpoint(MessageProcessor processor, String expectedAddress)
    {
        assertTrue(processor instanceof MessageProcessorChain);
        MessageProcessorChain chain = (MessageProcessorChain) processor;

        MessageProcessor firstInChain = chain.getMessageProcessors().get(0);
        assertTrue(firstInChain instanceof OutboundEndpoint);
        OutboundEndpoint endpoint = (OutboundEndpoint) firstInChain;

        String address = endpoint.getAddress();
        assertEquals(expectedAddress, address);
    }
}
