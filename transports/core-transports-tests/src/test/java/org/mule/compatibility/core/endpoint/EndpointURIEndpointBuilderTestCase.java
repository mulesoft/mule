/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.util.TransportObjectNameHelper;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.retry.policies.NoRetryPolicyTemplate;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;
import org.mule.tck.testmodels.mule.TestConnector;

import org.junit.Test;

public class EndpointURIEndpointBuilderTestCase extends AbstractMuleContextEndpointTestCase
{
    @Test
    public void testBuildInboundEndpoint() throws Exception
    {
        String uri = "test://address";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        ImmutableEndpoint ep = endpointBuilder.buildInboundEndpoint();
        assertTrue(ep instanceof InboundEndpoint);
        assertFalse(ep instanceof OutboundEndpoint);
        assertNotNull(ep.getMessageProcessors());
        // We no longer apply default transport transformers as part of endpoint processing
        assertEquals(0, ep.getMessageProcessors().size());
        assertNotNull(ep.getResponseMessageProcessors());
        // We no longer apply default transport transformers as part of endpoint processing
        assertEquals(0, ep.getResponseMessageProcessors().size());
        testDefaultCommonEndpointAttributes(ep);
    }

    @Test
    public void testBuildOutboundEndpoint() throws MuleException
    {
        String uri = "test://address";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        ImmutableEndpoint ep = endpointBuilder.buildOutboundEndpoint();
        assertFalse(ep instanceof InboundEndpoint);
        assertTrue(ep instanceof OutboundEndpoint);
        // We no longer apply default transport transformers as part of endpoint processing
        assertEquals(0, ep.getMessageProcessors().size());
        assertNotNull(ep.getResponseMessageProcessors());
        // We no longer apply default transport transformers as part of endpoint processing
        assertEquals(0, ep.getResponseMessageProcessors().size());
        testDefaultCommonEndpointAttributes(ep);
    }

    // TODO DF: Test more than defaults with tests using builder to set non-default
    // values

    protected void testDefaultCommonEndpointAttributes(ImmutableEndpoint ep)
    {
        assertEquals(ep.getEndpointURI().getUri().toString(), "test://address");
        assertEquals(muleContext.getConfiguration().getDefaultResponseTimeout(), ep.getResponseTimeout());
        assertTrue("ep.getRetryPolicyTemplate() = " + ep.getRetryPolicyTemplate().getClass(), ep.getRetryPolicyTemplate() instanceof NoRetryPolicyTemplate);
        assertTrue(ep.getTransactionConfig() instanceof MuleTransactionConfig);
        assertTrue(ep.getTransactionConfig() instanceof MuleTransactionConfig);
        assertEquals(null, ep.getSecurityFilter());
        assertTrue(ep.getConnector() instanceof TestConnector);
        assertEquals(new TransportObjectNameHelper(muleContext).getEndpointName(ep.getEndpointURI()), ep.getName());
        assertFalse(ep.isDeleteUnacceptedMessages());
        assertEquals(muleContext.getConfiguration().getDefaultEncoding(), ep.getEncoding());
        assertEquals(null, ep.getFilter());
        assertEquals(ImmutableEndpoint.INITIAL_STATE_STARTED, ep.getInitialState());
    }
    
    @Test
    public void testHasSetEncodingMethod() throws Exception
    {
        String uri = "test://address";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        assertNotNull(endpointBuilder.getClass().getMethod("setEncoding", new Class[]{String.class}));
    }
    
    @Test
    public void testEndpointBuilderFromEndpoint() throws Exception
    {
        String uri = "test://address";
        ImmutableEndpoint endpoint = getTestInboundEndpoint("endpoint.test.address", uri);
        SensingEndpointURIEndpointBuilder builder = new SensingEndpointURIEndpointBuilder(endpoint);
        assertEquals(uri, builder.getEndpointBuilder().getEndpoint().getUri().toString());
        assertEquals(endpoint.getConnector(), builder.getConnector());
        assertEquals(endpoint.getProperties(), builder.getProperties());
        assertEquals(endpoint.getTransactionConfig(), builder.getTransactionConfig());
        assertEquals(endpoint.isDeleteUnacceptedMessages(), builder.getDeleteUnacceptedMessages(builder.getConnector()));
        assertEquals(endpoint.getInitialState(), builder.getInitialState(builder.getConnector()));
        assertEquals(endpoint.getResponseTimeout(), builder.getResponseTimeout(builder.getConnector()));
        assertEquals(endpoint.getSecurityFilter(), builder.getSecurityFilter());
        assertEquals(endpoint.getRetryPolicyTemplate(), builder.getRetryPolicyTemplate(builder.getConnector()));
        assertEquals(MessageExchangePattern.ONE_WAY, builder.getExchangePattern());
    }
    
    /**
     * Assert that the builder state (message prococessors/transformers) doesn't change when endpont is built
     * multiple times
     * 
     * @throws Exception
     */
    @Test
    public void testEndpointBuilderTransformersState() throws Exception
    {
        muleContext.getRegistry().registerObject("tran1", new StringAppendTransformer("1"));
        muleContext.getRegistry().registerObject("tran2", new StringAppendTransformer("2"));

        String uri = "test://address?transformers=tran1&responseTransformers=tran2";
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        endpointBuilder.setTransformers(java.util.Collections.<Transformer> singletonList(new StringAppendTransformer(
            "3")));
        endpointBuilder.setResponseTransformers(java.util.Collections.<Transformer> singletonList(new StringAppendTransformer(
            "4")));

        InboundEndpoint endpoint = endpointBuilder.buildInboundEndpoint();

        assertEquals(2, endpoint.getMessageProcessors().size());
        assertEquals(2, endpoint.getResponseMessageProcessors().size());

        endpoint = endpointBuilder.buildInboundEndpoint();

        assertEquals(2, endpoint.getMessageProcessors().size());
        assertEquals(2, endpoint.getResponseMessageProcessors().size());
    }
    
    private static class SensingEndpointURIEndpointBuilder extends EndpointURIEndpointBuilder
    {
        public SensingEndpointURIEndpointBuilder(ImmutableEndpoint endpoint)
        {
            super(endpoint);
        }
        
        public MessageExchangePattern getExchangePattern()
        {
            return messageExchangePattern;
        }
    }
}
