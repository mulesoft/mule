/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;

import com.mockobjects.dynamic.Mock;

import javax.jms.Queue;
import javax.jms.Topic;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DefaultJmsTopicResolverTestCase extends FunctionalTestCase
{
    private JmsConnector connector;
    private DefaultJmsTopicResolver resolver;

    @Override
    protected String getConfigResources()
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
        Mock mock = new Mock(Queue.class);
        Queue queue = (Queue) mock.proxy();

        assertFalse(resolver.isTopic(queue));
        mock.verify();
    }

    @Test
    public void testDestinationTopic() throws Exception
    {
        // prepare the mock
        Mock mock = new Mock(Topic.class);
        Topic topic = (Topic) mock.proxy();

        assertTrue(resolver.isTopic(topic));
        mock.verify();
    }

}
