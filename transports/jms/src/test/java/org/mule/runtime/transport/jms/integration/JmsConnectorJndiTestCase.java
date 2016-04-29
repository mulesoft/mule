/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

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
    protected String getConfigFile()
    {
        return "integration/jms-jndi-config.xml";
    }

    @Test
    public void testConnectionFactoryFromJndi() throws Exception
    {
        // No need to specifically test anything here, if the ConnectionFactory
        // is not successfully looked up from JNDI, Mule won't even start up.
    }

    @Test
    public void testQueueFromJndi() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_jndi-queue", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(result));
    }

    @Test
    public void testTopicFromJndi() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_jndi-topic", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(result));
    }

    /**
     * Use a non-JNDI Destination when jndiDestinations="false", test should pass.
     */
    @Test
    public void testNonJndiDestination() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_non-jndi-queue", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(result));
    }

    /**
     * Use a non-JNDI Destination when jndiDestinations="true" but forceJndiDestinations="false", test should pass.
     */
    @Test
    public void testNonJndiDestinationOptional() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_non-jndi-queue-optional-jndi", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(result));
    }

    /**
     * Use a non-JNDI Destination when forceJndiDestinations="true", test should fail.
     */
    @Test
    public void testNonJndiDestinationForce() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_non-jndi-queue-force-jndi", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNull("Attempt to look up a non-existant JNDI Destination should have failed", result);
    }

    @Test
    public void testQueueFromJndiWithJndiNameResolver() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_jndi-queue-with-jndi-name-resolver", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(result));
    }

    @Test
    public void testTopicFromJndiWithJndiNameResolver() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_jndi-topic-with-jndi-name-resolver", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(result));
    }

    /**
    * Use a non-JNDI Destination when jndiDestinations="false", test should pass.
    */
    @Test
    public void testNonJndiDestinationWithJndiNameResolver() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_non-jndi-queue-with-jndi-name-resolver", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(result));
    }

    /**
    * Use a non-JNDI Destination when jndiDestinations="true" but forceJndiDestinations="false", test should pass.
    */
    @Test
    public void testNonJndiDestinationOptionalWithJndiNameResolver() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_non-jndi-queue-optional-jndi-with-jndi-name-resolver", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(result));
    }

    /**
    * Use a non-JNDI Destination when forceJndiDestinations="true", test should fail.
    */
    @Test
    public void testNonJndiDestinationForceWithJndiNameResolver() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("ep_non-jndi-queue-force-jndi-with-jndi-name-resolver", DEFAULT_INPUT_MESSAGE, null);

        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNull("Attempt to look up a non-existant JNDI Destination should have failed", result);
    }
}
