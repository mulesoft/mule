/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import org.junit.Test;

/**
 * Requires the following connector config: <jms:connector name="jmsConnector1"
 * jndiInitialFactory="org.apache.activemq.jndi.ActiveMQInitialContextFactory"
 * jndiProviderUrl="vm://localhost?broker.persistent=false&amp;broker.useJmx=false"
 * connectionFactoryJndiName="ConnectionFactory" /> <jms:xxx-connector
 * name="jmsConnector2"
 * jndiInitialFactory="org.apache.activemq.jndi.ActiveMQInitialContextFactory"
 * jndiProviderUrl="vm://localhost?broker.persistent=false&amp;broker.useJmx=false"
 * jndiProviderProperties-ref="providerProperties" jndiDestinations="true"
 * forceJndiDestinations="true" connectionFactoryJndiName="ConnectionFactory" />
 * <spring:beans> <util:properties id="providerProperties"> <!-- see
 * http://activemq.apache.org/jndi-support.html for configuring queues/topics through
 * JNDI properties for other Jms vendors these JNDI properties will need to be
 * available from the JNDI context --> <spring:prop key="queue.in2">in-queue2</spring:prop>
 * <spring:prop key="topic.some/long/path/in3">in-topic3</spring:prop>
 * </util:properties> </spring:beans>
 */
public class JmsConnectorJndiTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "integration/jms-jndi-config.xml";
    }

    @Test
    public void testConnectionFactoryFromJndi() throws Exception
    {
        MuleClient client = new MuleClient();

        client.dispatch("jms://in1?connector=jmsConnector1", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, result.getPayloadAsString());
    }

    @Test
    public void testQueueFromJndi() throws Exception
    {
        MuleClient client = new MuleClient();

        client.dispatch("jms://jndi-queue-in?connector=jmsConnector2", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, result.getPayloadAsString());
    }

    @Test
    public void testTopicFromJndi() throws Exception
    {
        MuleClient client = new MuleClient();

        client.dispatch("jms://topic:jndi-topic-in?connector=jmsConnector2", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, result.getPayloadAsString());
    }
}
