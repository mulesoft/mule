/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.endpoint.MuleEndpointURI;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class JmsEndpointTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testWithoutFullUrl() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms:/my.queue", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("my.queue", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals("jms:/my.queue", url.toString());
    }

    @Test
    public void testFullUrlWithSlashes() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://my/queue", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("my/queue", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals("jms://my/queue", url.toString());
    }

    @Test
    public void testWithoutFullUrlAndEndpointName() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms:/my.queue?endpointName=jmsProvider", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("my.queue", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("jmsProvider", url.getEndpointName());
        assertEquals("jms:/my.queue?endpointName=jmsProvider", url.toString());
    }

    @Test
    public void testJmsUrl() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://queue1?endpointName=jmsProvider", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("queue1", url.getAddress());
        assertEquals("jmsProvider", url.getEndpointName());
        assertEquals("jms://queue1?endpointName=jmsProvider", url.toString());
    }


    @Test
    public void testJmsTopic() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://topic:topic1", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("topic1", url.getAddress());
        assertEquals("topic", url.getResourceInfo());
        assertEquals(null, url.getEndpointName());
        assertEquals("jms://topic:topic1", url.toString());
    }

    @Test
    public void testJmsTopicWithProvider() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://topic:topic1?endpointName=jmsProvider", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("topic1", url.getAddress());
        assertEquals("jmsProvider", url.getEndpointName());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("jms://topic:topic1?endpointName=jmsProvider", url.toString());
    }

    @Test
    public void testJmsTopicWithUserInfo() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://user:password@topic:topic1", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("topic1", url.getAddress());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("user", url.getUser());
        assertEquals("password", url.getPassword());
        assertEquals("jms://user:****@topic:topic1", url.toString());
    }

    @Test
    public void testJmsTopicWithUserInfoAndProvider() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://user:password@topic:topic1?endpointName=jmsProvider", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("topic1", url.getAddress());
        assertEquals("jmsProvider", url.getEndpointName());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("user", url.getUser());
        assertEquals("password", url.getPassword());
        assertEquals("jms://user:****@topic:topic1?endpointName=jmsProvider", url.toString());
    }

    @Test
    public void testJmsDestWithSlashesAndUserInfoUsingAddressParam() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://user:password@?address=/myQueues/myQueue", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("/myQueues/myQueue", url.getAddress());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("user", url.getUser());
        assertEquals("password", url.getPassword());
        assertEquals("jms://user:****@?address=/myQueues/myQueue", url.toString());
    }

    @Test
    public void testJmsDestWithSlashesAndUserInfo() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://user:password@myQueues/myQueue", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("myQueues/myQueue", url.getAddress());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("user", url.getUser());
        assertEquals("password", url.getPassword());
        assertEquals("jms://user:****@myQueues/myQueue", url.toString());
    }

    @Test
    public void testJmsTopicDestinationsWithAddressParam() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms:topic://?address=[[testgroup]]test.topic", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("[[testgroup]]test.topic", url.getAddress());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("jms://?address=[[testgroup]]test.topic", url.toString());
    }

    @Test
    public void testJmsQueueDestinationsWithAddressParam() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://?address=[[testgroup]]test.queue", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("[[testgroup]]test.queue", url.getAddress());
        assertNull( url.getResourceInfo());
        assertEquals("jms://?address=[[testgroup]]test.queue", url.toString());
    }

    @Test
    public void testJmsQueueDestinationsWithEncoding() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://%5B%5Btestgroup%5D%5Dtest.queue", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("[[testgroup]]test.queue", url.getAddress());
        assertNull( url.getResourceInfo());
        assertEquals("jms://%5B%5Btestgroup%5D%5Dtest.queue", url.toString());
    }

    @Test
    public void testJmsTopicDestinationsWithEncoding() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms:topic://%5B%5Btestgroup%5D%5Dtest.topic", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("[[testgroup]]test.topic", url.getAddress());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("jms://%5B%5Btestgroup%5D%5Dtest.topic", url.toString());
    }

    @Test
    public void testJmsLegacyTopicDestinationsWithEncoding() throws Exception
    {
        EndpointURI url = new MuleEndpointURI("jms://topic:%5B%5Btestgroup%5D%5Dtest.topic", muleContext);
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("[[testgroup]]test.topic", url.getAddress());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("jms://topic:%5B%5Btestgroup%5D%5Dtest.topic", url.toString());
    }


}
