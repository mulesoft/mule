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

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.TransportFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

public class JmsEndpointTestCase extends AbstractMuleTestCase
{

    public void testWithoutFullUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms:/my.queue");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("my.queue", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals("jms:/my.queue", url.toString());
    }

    public void testFullUrlWithSlashes() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://my/queue");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("my/queue", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals("jms://my/queue", url.toString());
    }

    public void testWithoutFullUrlAndEndpointName() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms:/my.queue?endpointName=jmsProvider");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("my.queue", url.getAddress());
        assertNotNull(url.getEndpointName());
        assertEquals("jmsProvider", url.getEndpointName());
        assertEquals("jms:/my.queue?endpointName=jmsProvider", url.toString());
    }

    public void testJmsUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://queue1?endpointName=jmsProvider");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("queue1", url.getAddress());
        assertEquals("jmsProvider", url.getEndpointName());
        assertEquals("jms://queue1?endpointName=jmsProvider", url.toString());
    }


    public void testJmsTopic() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://topic:topic1");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("topic1", url.getAddress());
        assertEquals("topic", url.getResourceInfo());
        assertEquals(null, url.getEndpointName());
        assertEquals("jms://topic:topic1", url.toString());
    }

    public void testJmsTopicWithProvider() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://topic:topic1?endpointName=jmsProvider");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("topic1", url.getAddress());
        assertEquals("jmsProvider", url.getEndpointName());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("jms://topic:topic1?endpointName=jmsProvider", url.toString());
    }

    public void testJmsTopicWithUserInfo() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://user:password@topic:topic1");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("topic1", url.getAddress());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("user", url.getUsername());
        assertEquals("password", url.getPassword());
        assertEquals("jms://user:password@topic:topic1", url.toString());
    }

    public void testJmsTopicWithUserInfoAndProvider() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://user:password@topic:topic1?endpointName=jmsProvider");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("topic1", url.getAddress());
        assertEquals("jmsProvider", url.getEndpointName());
        assertEquals("topic", url.getResourceInfo());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("user", url.getUsername());
        assertEquals("password", url.getPassword());
        assertEquals("jms://user:password@topic:topic1?endpointName=jmsProvider", url.toString());
    }

    public void testJmsDestWithSlashesAndUserInfoUsingAddressParam() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://user:password@?address=/myQueues/myQueue&createConnector=ALWAYS");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("/myQueues/myQueue", url.getAddress());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("user", url.getUsername());
        assertEquals("password", url.getPassword());
        assertEquals(TransportFactory.ALWAYS_CREATE_CONNECTOR, url.getCreateConnector());
        assertEquals("jms://user:password@?address=/myQueues/myQueue&createConnector=ALWAYS", url.toString());
    }

    public void testJmsDestWithSlashesAndUserInfo() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://user:password@myQueues/myQueue?createConnector=ALWAYS");
        url.initialise();
        assertEquals("jms", url.getScheme());
        assertEquals("myQueues/myQueue", url.getAddress());
        assertEquals("user:password", url.getUserInfo());
        assertEquals("user", url.getUsername());
        assertEquals("password", url.getPassword());
        assertEquals(TransportFactory.ALWAYS_CREATE_CONNECTOR, url.getCreateConnector());
        assertEquals("jms://user:password@myQueues/myQueue?createConnector=ALWAYS", url.toString());
    }
}
