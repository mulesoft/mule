/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;

import javax.jms.Queue;
import javax.jms.Topic;

import org.junit.Test;

public class DefaultJmsTopicResolverTestCase extends FunctionalTestCase
{
    private JmsConnector connector;
    private DefaultJmsTopicResolver resolver;

    @Override
    protected String getConfigFile()
    {
        return "jms-topic-resolver.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector");
        resolver = (DefaultJmsTopicResolver) connector.getTopicResolver();
    }

    @Test
    public void testSameConnector()
    {
        assertSame(connector, resolver.getConnector());
    }

    @Test
    public void testEndpointNotTopicWithFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep1");
        assertFalse(resolver.isTopic(endpoint));
    }

    @Test
    public void testEndpointNotTopicWithFallback2() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep1");
        assertFalse(resolver.isTopic(endpoint, true));
    }

    @Test
    public void testEndpointNotTopicNoFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep1");
        assertFalse(resolver.isTopic(endpoint, false));
    }

    @Test
    public void testEndpointTopicPropertyWithFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep2");
        assertTrue(resolver.isTopic(endpoint));
    }

    @Test
    public void testEndpointTopicPropertyWithFallback2() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep2");
        assertTrue(resolver.isTopic(endpoint, true));
    }

    @Test
    public void testEndpointTopicPropertyNoFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep2");
        assertFalse(resolver.isTopic(endpoint, false));
    }

    @Test
    public void testEndpointTopicPrefixWithFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep3");
        assertTrue(resolver.isTopic(endpoint));
    }

    @Test
    public void testEndpointTopicPrefixWithFallback2() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep3");
        assertTrue(resolver.isTopic(endpoint, true));
    }

    @Test
    public void testEndpointTopicPrefixNoFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep3");
        assertTrue(resolver.isTopic(endpoint, false));
    }

    @Test
    public void testEndpointTopicPrefixAndPropertyWithFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep4");
        assertTrue(resolver.isTopic(endpoint));
    }

    @Test
    public void testEndpointTopicPrefixAndPropertyWithFallback2() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep4");
        assertTrue(resolver.isTopic(endpoint, true));
    }

    @Test
    public void testEndpointTopicPrefixAndPropertyNoFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep4");
        assertTrue(resolver.isTopic(endpoint, false));
    }

    @Test
    public void testEndpointTopicUsesEndpointProperties() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep5");
        assertTrue(resolver.isTopic(endpoint));
    }

    @Test
    public void testEndpointTopicWithLeadingSlash() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep6");
        assertTrue(resolver.isTopic(endpoint));
    }

    @Test
    public void testEndpointTopicWithSlashes() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep7");
        assertTrue(resolver.isTopic(endpoint));
    }

    @Test
    public void testEndpointQueueWithSlashes() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep8");
        assertFalse(resolver.isTopic(endpoint));
    }

    @Test
    public void testDestinationNotTopic() throws Exception
    {
        // prepare the mock
        Queue queue = mock(Queue.class);

        assertFalse(resolver.isTopic(queue));
    }

    @Test
    public void testDestinationTopic() throws Exception
    {
        // prepare the mock
        Topic topic = mock(Topic.class);

        assertTrue(resolver.isTopic(topic));
    }
}
