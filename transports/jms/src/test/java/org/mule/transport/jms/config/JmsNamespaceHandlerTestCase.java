/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.config;

import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.construct.Flow;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsConstants;
import org.mule.transport.jms.filters.JmsPropertyFilter;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.transport.jms.test.TestConnectionFactory;
import org.mule.transport.jms.test.TestRedeliveryHandler;
import org.mule.transport.jms.test.TestRedeliveryHandlerFactory;

import java.util.Arrays;
import java.util.Collection;

import javax.jms.Session;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the "jms" namespace.
 */
public class JmsNamespaceHandlerTestCase extends AbstractServiceAndFlowTestCase
{
    public JmsNamespaceHandlerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setStartContext(false);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "jms-namespace-config-service.xml"},
            {ConfigVariant.FLOW, "jms-namespace-config-flow.xml"}});
    }

    @Test
    public void testDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnectorDefaults");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());
        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.AUTO_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertNull(c.getUsername());
        assertNull(c.getPassword());

        assertNotNull(c.getRedeliveryHandlerFactory());
        assertTrue(c.getRedeliveryHandlerFactory() instanceof TestRedeliveryHandlerFactory);
        assertTrue(c.getRedeliveryHandlerFactory().create() instanceof TestRedeliveryHandler);

        assertNull(c.getClientId());
        assertFalse(c.isDurable());
        assertFalse(c.isNoLocal());
        assertFalse(c.isPersistentDelivery());
        assertEquals(0, c.getMaxRedelivery());
        assertTrue(c.isCacheJmsSessions());
        assertTrue(c.isEagerConsumer());
        assertEquals(4, c.getNumberOfConcurrentTransactedReceivers());
        assertFalse(c.isEmbeddedMode());
    }

    @Test
    public void testConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector1");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());

        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertEquals("myuser", c.getUsername());
        assertEquals("mypass", c.getPassword());

        assertNotNull(c.getRedeliveryHandlerFactory());
        assertTrue(c.getRedeliveryHandlerFactory().create() instanceof TestRedeliveryHandler);

        assertEquals("myClient", c.getClientId());
        assertTrue(c.isDurable());
        assertTrue(c.isNoLocal());
        assertTrue(c.isPersistentDelivery());
        assertEquals(5, c.getMaxRedelivery());
        assertTrue(c.isCacheJmsSessions());
        assertFalse(c.isEagerConsumer());

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should
                                                   // be changed in the config
        // test properties, default is 4
        assertEquals(7, c.getNumberOfConcurrentTransactedReceivers());
        assertTrue(c.isEmbeddedMode());
    }

    @Test
    public void testCustomConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector2");
        assertNotNull(c);

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should
                                                   // be changed in the config
    }

    @Test
    public void testTestConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector3");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());

        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());

        assertNotNull(c.getRedeliveryHandlerFactory());
        assertTrue(c.getRedeliveryHandlerFactory().create() instanceof TestRedeliveryHandler);

        assertEquals("myClient", c.getClientId());
        assertTrue(c.isDurable());
        assertTrue(c.isNoLocal());
        assertTrue(c.isPersistentDelivery());
        assertEquals(5, c.getMaxRedelivery());
        assertTrue(c.isCacheJmsSessions());
        assertFalse(c.isEagerConsumer());

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should
                                                   // be changed in the config
    }

    @Test
    public void testEndpointConfig() throws MuleException
    {
        ImmutableEndpoint endpoint1 = muleContext.getRegistry()
            .lookupEndpointBuilder("endpoint1")
            .buildInboundEndpoint();
        assertNotNull(endpoint1);
        Filter filter1 = endpoint1.getFilter();
        assertNotNull(filter1);
        assertTrue(filter1 instanceof JmsSelectorFilter);
        assertEquals(1, endpoint1.getProperties().size());
        assertEquals("true", endpoint1.getProperty(JmsConstants.DISABLE_TEMP_DESTINATIONS_PROPERTY));

        ImmutableEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointBuilder("endpoint2")
            .buildOutboundEndpoint();
        assertNotNull(endpoint2);
        Filter filter2 = endpoint2.getFilter();
        assertNotNull(filter2);
        assertTrue(filter2 instanceof NotFilter);
        Filter filter3 = ((NotFilter) filter2).getFilter();
        assertNotNull(filter3);
        assertTrue(filter3 instanceof JmsPropertyFilter);

        InboundEndpoint inboundEndpoint;

        if (variant.equals(ConfigVariant.FLOW))
        {
            inboundEndpoint = (InboundEndpoint) ((Flow) muleContext.getRegistry()
                            .lookupObject("testService"))
                            .getMessageSource();
        }
        else
        {
            inboundEndpoint = ((ServiceCompositeMessageSource) muleContext.getRegistry()
                .lookupService("testService")
                .getMessageSource()).getEndpoints().get(0);
        }

        assertNotNull(inboundEndpoint);
        assertEquals(1, inboundEndpoint.getProperties().size());
        assertEquals("testCustomDurableName", inboundEndpoint.getProperty(JmsConstants.DURABLE_NAME_PROPERTY));
    }

    @Test
    public void testCustomTransactions() throws EndpointException, InitialisationException
    {
        ImmutableEndpoint endpoint3 = muleContext.getRegistry()
            .lookupEndpointBuilder("endpoint3")
            .buildInboundEndpoint();
        assertNotNull(endpoint3);
        TestTransactionFactory factory = (TestTransactionFactory) endpoint3.getTransactionConfig()
            .getFactory();
        assertNotNull(factory);
        assertEquals("foo", factory.getValue());
    }

    @Test
    public void testXaTransactions() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointBuilder("endpoint4")
            .buildInboundEndpoint();
        assertNotNull(endpoint);
        assertEquals(XaTransactionFactory.class, endpoint.getTransactionConfig().getFactory().getClass());
        assertEquals(MuleTransactionConfig.ACTION_ALWAYS_JOIN, endpoint.getTransactionConfig().getAction());
    }

    @Test
    public void testJndiConnectorAtributes() throws Exception
    {
        JmsConnector connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsJndiConnector");
        assertThat("connection factory must be created only after connect so reconnection works when JNDI context is not yet available during start", connector.getConnectionFactory(), nullValue());
        connector.connect();
        assertNotNull(connector);

        assertEquals("org.mule.transport.jms.test.JmsTestContextFactory", connector.getJndiInitialFactory());
        assertEquals("jndi://test", connector.getJndiProviderUrl());
        assertEquals("jms/connectionFactory", connector.getConnectionFactoryJndiName());
        assertEquals("org.mule.transport.jms.test.TestConnectionFactory", connector.getConnectionFactory()
            .getClass()
            .getName());
        assertTrue(connector.isJndiDestinations());
        assertTrue(connector.isForceJndiDestinations());
        assertEquals("value", connector.getJndiProviderProperties().get("key"));
        assertEquals("customValue", connector.getConnectionFactoryProperties().get("customProperty"));
        assertEquals("customValue",
            ((TestConnectionFactory) connector.getConnectionFactory()).getCustomProperty());
    }

    @Test
    public void testActiveMqConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsActiveMqConnector");
        assertNotNull(c);

        assertEquals(1, c.getNumberOfConsumers());

        c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsActiveMqConnectorXa");
        assertNotNull(c);

        assertEquals(1, c.getNumberOfConsumers());
    }
}
