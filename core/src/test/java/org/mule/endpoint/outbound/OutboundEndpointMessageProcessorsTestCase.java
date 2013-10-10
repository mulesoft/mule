/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for configuring message processors on an outbound endpoint.
 */
public class OutboundEndpointMessageProcessorsTestCase extends AbstractMessageProcessorTestCase
{
    private MuleEvent testOutboundEvent;
    private OutboundEndpoint endpoint;
    private MuleEvent result;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        endpoint = createOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        testOutboundEvent = createTestOutboundEvent();
    }

    @Test
    public void testProcessors() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestMessageProcessor("1"), new TestMessageProcessor("2"), new TestMessageProcessor("3"));
        MessageProcessor mpChain = builder.build();
        
        result = mpChain.process(testOutboundEvent);
        assertEquals(TEST_MESSAGE + ":1:2:3", result.getMessage().getPayload());
    }

    @Test
    public void testNoProcessors() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        MessageProcessor mpChain = builder.build();
        
        result = mpChain.process(testOutboundEvent);
        assertEquals(TEST_MESSAGE, result.getMessage().getPayload());
    }

    protected OutboundEndpoint createOutboundEndpoint(Filter filter,
                                                      EndpointSecurityFilter securityFilter,
                                                      Transformer in,
                                                      Transformer response,
                                                      MessageExchangePattern exchangePattern,
                                                      TransactionConfig txConfig) throws Exception
    {
        return createTestOutboundEndpoint(filter, securityFilter, in, response, exchangePattern, 
            txConfig);
    }
}
