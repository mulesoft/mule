/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.processor.builder.ChainMessageProcessorBuilder;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

/**
 * Unit test for configuring message processors on an outbound endpoint.
 */
public class OutboundEndpointMessageProcessorsTestCase extends AbstractOutboundMessageProcessorTestCase
{
    private MuleEvent testOutboundEvent;
    private OutboundEndpoint endpoint;
    private MuleEvent result;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        endpoint = createOutboundEndpoint(null, null, null, null, true, null);
        testOutboundEvent = createTestOutboundEvent(endpoint);
    }

    public void testProcessors() throws Exception
    {
        ChainMessageProcessorBuilder builder = new ChainMessageProcessorBuilder();
        builder.chain(new TestMessageProcessor("1"), new TestMessageProcessor("2"), new TestMessageProcessor("3"));
        MessageProcessor mpChain = builder.build();
        
        result = mpChain.process(testOutboundEvent);
        assertEquals(TEST_MESSAGE + ":1:2:3", result.getMessage().getPayload());
    }

    public void testNoProcessors() throws Exception
    {
        ChainMessageProcessorBuilder builder = new ChainMessageProcessorBuilder();
        MessageProcessor mpChain = builder.build();
        
        result = mpChain.process(testOutboundEvent);
        assertEquals(TEST_MESSAGE, result.getMessage().getPayload());
    }

    protected OutboundEndpoint createOutboundEndpoint(Filter filter,
                                                      EndpointSecurityFilter securityFilter,
                                                      Transformer in,
                                                      Transformer response,
                                                      boolean sync,
                                                      TransactionConfig txConfig) throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(filter, securityFilter, in, response, sync, txConfig);
        Connector connector = endpoint.getConnector();
        connector.start();
        return endpoint;
    }
}
