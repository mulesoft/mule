/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import javax.jms.Queue;
import javax.jms.Topic;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import com.mockobjects.dynamic.Mock;

public class DefaultJmsTopicResolverTestCase extends FunctionalTestCase
{
    private JmsConnector connector;
    private DefaultJmsTopicResolver resolver;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        connector = (JmsConnector) managementContext.getRegistry().lookupConnector("jmsConnector");
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
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep1", managementContext);
        assertFalse(resolver.isTopic(endpoint));
    }

    public void testEndpointNotTopicWithFallback2() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep1", managementContext);
        assertFalse(resolver.isTopic(endpoint, true));
    }

    public void testEndpointNotTopicNoFallback() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep1", managementContext);
        assertFalse(resolver.isTopic(endpoint, false));
    }

    public void testEndpointTopicPropertyWithFallback() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep2", managementContext);
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointTopicPropertyWithFallback2() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep2", managementContext);
        assertTrue(resolver.isTopic(endpoint, true));
    }

    public void testEndpointTopicPropertyNoFallback() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep2", managementContext);
        assertFalse(resolver.isTopic(endpoint, false));
    }

    public void testEndpointTopicPrefixWithFallback() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep3", managementContext);
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointTopicPrefixWithFallback2() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep3", managementContext);
        assertTrue(resolver.isTopic(endpoint, true));
    }

    public void testEndpointTopicPrefixNoFallback() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep3", managementContext);
        assertTrue(resolver.isTopic(endpoint, false));
    }

    public void testEndpointTopicPrefixAndPropertyWithFallback() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep4", managementContext);
        assertTrue(resolver.isTopic(endpoint));
    }

    public void testEndpointTopicPrefixAndPropertyWithFallback2() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep4", managementContext);
        assertTrue(resolver.isTopic(endpoint, true));
    }

    public void testEndpointTopicPrefixAndPropertyNoFallback() throws Exception
    {
        UMOImmutableEndpoint endpoint = managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ep4", managementContext);
        assertTrue(resolver.isTopic(endpoint, false));
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
