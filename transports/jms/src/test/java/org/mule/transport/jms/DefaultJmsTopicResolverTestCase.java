/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.FunctionalTestCase;

import com.mockobjects.dynamic.Mock;

import javax.jms.Queue;
import javax.jms.Topic;

public class DefaultJmsTopicResolverTestCase extends FunctionalTestCase
{
    private JmsConnector connector;
    private DefaultJmsTopicResolver resolver;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector");
        resolver = (DefaultJmsTopicResolver) connector.getTopicResolver();
    }

    protected String getConfigResources()
    {
        return "jms-topic-resolver.xml";
    }

    public void testSameConnector()
    {
        assertSame(connector, resolver.getConnector());
    }

    public void testEndpointNotTopicWithFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep1");
        assertFalse(resolver.isTopic(endpoint));
    }

    public void testEndpointNotTopicWithFallback2() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep1");
        assertFalse(resolver.isTopic(endpoint, true));
    }

    public void testEndpointNotTopicNoFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep1");
        assertFalse(resolver.isTopic(endpoint, false));
    }

    public void testEndpointTopicPropertyWithFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep2");
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointTopicPropertyWithFallback2() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep2");
        assertTrue(resolver.isTopic(endpoint, true));
    }

    public void testEndpointTopicPropertyNoFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep2");
        assertFalse(resolver.isTopic(endpoint, false));
    }

    public void testEndpointTopicPrefixWithFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep3");
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointTopicPrefixWithFallback2() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep3");
        assertTrue(resolver.isTopic(endpoint, true));
    }

    public void testEndpointTopicPrefixNoFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep3");
        assertTrue(resolver.isTopic(endpoint, false));
    }

    public void testEndpointTopicPrefixAndPropertyWithFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep4");
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointTopicPrefixAndPropertyWithFallback2() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep4");
        assertTrue(resolver.isTopic(endpoint, true));
    }

    public void testEndpointTopicPrefixAndPropertyNoFallback() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep4");
        assertTrue(resolver.isTopic(endpoint, false));
    }

    public void testEndpointTopicUsesEndpointProperties() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep5");
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointTopicWithLeadingSlash() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getInboundEndpoint("ep6");
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointTopicWithSlashes() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getInboundEndpoint("ep7");
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointQueueWithSlashes() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getInboundEndpoint("ep8");
        assertFalse(resolver.isTopic(endpoint));
    }


    public void testDestinationNotTopic() throws Exception
    {
        // prepare the mock
        Mock mock = new Mock(Queue.class);
        Queue queue = (Queue) mock.proxy();

        assertFalse(resolver.isTopic(queue));
        mock.verify();
    }

    public void testDestinationTopic() throws Exception
    {
        // prepare the mock
        Mock mock = new Mock(Topic.class);
        Topic topic = (Topic) mock.proxy();

        assertTrue(resolver.isTopic(topic));
        mock.verify();
    }

}
