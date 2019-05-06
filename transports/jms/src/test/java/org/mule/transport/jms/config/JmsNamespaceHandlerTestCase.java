/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
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

import org.hamcrest.CoreMatchers;
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
    public void testDefaultConfig()
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnectorDefaults");
        assertThat(c, is(notNullValue()));

        assertThat(c.getConnectionFactory(), is(notNullValue()));

        assertThat(c.getConnectionFactory(), is(notNullValue()));
        assertThat(c.getConnectionFactory(), is(instanceOf(TestConnectionFactory.class)));
        assertThat(Session.AUTO_ACKNOWLEDGE, is(c.getAcknowledgementMode()));
        assertThat(c.getUsername(), is(nullValue()));
        assertThat(c.getPassword(), is(nullValue()));

        assertThat(c.getRedeliveryHandlerFactory(), is(notNullValue()));
        assertThat(c.getRedeliveryHandlerFactory(), is(instanceOf(TestRedeliveryHandlerFactory.class)));
        assertThat(c.getRedeliveryHandlerFactory().create(), is(instanceOf(TestRedeliveryHandler.class)));

        assertThat(c.getClientId(), is(nullValue()));
        assertThat(c.isDurable(), is(false));
        assertThat(c.isNoLocal(), is(false));
        assertThat(c.isPersistentDelivery(), is(true));
        assertThat(c.getMaxRedelivery(), is(0));
        assertThat(c.isCacheJmsSessions(), is(true));
        assertThat(c.isEagerConsumer(), is(true));
        assertThat(c.getNumberOfConcurrentTransactedReceivers(), is(4));
        assertThat(c.isEmbeddedMode(), is(false));
    }

    @Test
    public void testConnectorConfig()
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector1");
        assertThat(c, is(notNullValue()));

        assertThat(c.getConnectionFactory(), is(instanceOf(TestConnectionFactory.class)));
        assertThat(c.getAcknowledgementMode(), is(Session.DUPS_OK_ACKNOWLEDGE));
        assertThat(c.getUsername(), is("myuser"));
        assertThat(c.getPassword(), is("mypass"));

        assertThat(c.getRedeliveryHandlerFactory(), is(notNullValue()));
        assertThat(c.getRedeliveryHandlerFactory().create(), is(instanceOf(TestRedeliveryHandler.class)));

        assertThat(c.getClientId(), is("myClient"));
        assertThat(c.isDurable(), is(true));
        assertThat(c.isNoLocal(), is(true));
        assertThat(c.isPersistentDelivery(), is(true));
        assertThat(c.getMaxRedelivery(), is(5));
        assertThat(c.isCacheJmsSessions(), is(true));
        assertThat(c.isEagerConsumer(), is(false));

        assertThat(c.getSpecification(), is("1.1")); // 1.0.2b is the default, should
        // be changed in the config
        // test properties, default is 4
        assertThat(c.getNumberOfConcurrentTransactedReceivers(), is(7));
        assertThat(c.isEmbeddedMode(), is(true));
    }

    @Test
    public void testCustomConnectorConfig()
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector2");
        assertThat(c, is(notNullValue()));

        assertThat(c.getSpecification(), is("1.1")); // 1.0.2b is the default, should
                                                   // be changed in the config
    }

    @Test
    public void testTestConnectorConfig()
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector3");
        assertThat(c, is(notNullValue()));

        assertThat(c.getConnectionFactory(), is(notNullValue()));

        assertThat(c.getConnectionFactory(), is(instanceOf(TestConnectionFactory.class)));
        assertThat(Session.DUPS_OK_ACKNOWLEDGE, is(c.getAcknowledgementMode()));

        assertThat(c.getRedeliveryHandlerFactory(), is(notNullValue()));
        assertThat(c.getRedeliveryHandlerFactory().create(), is(instanceOf(TestRedeliveryHandler.class)));

        assertThat(c.getClientId(), is("myClient"));
        assertThat(c.isDurable(), is(true));
        assertThat(c.isNoLocal(), is(true));
        assertThat(c.isPersistentDelivery(), is(true));
        assertThat(c.getMaxRedelivery(), is(5));
        assertThat(c.isCacheJmsSessions(), is(true));
        assertThat(c.isEagerConsumer(), is(false));

        assertThat(c.getSpecification(), is("1.1")); // 1.0.2b is the default, should
                                                   // be changed in the config
    }

    @Test
    public void testEndpointConfig() throws MuleException
    {
        ImmutableEndpoint endpoint1 = muleContext.getRegistry()
            .lookupEndpointBuilder("endpoint1")
            .buildInboundEndpoint();
        assertThat(endpoint1, is(notNullValue()));
        Filter filter1 = endpoint1.getFilter();
        assertThat(filter1, is(notNullValue()));
        assertThat(filter1, is(instanceOf(JmsSelectorFilter.class)));
        assertThat(endpoint1.getProperties().size(), is(1));
        assertThat(endpoint1.getProperty(JmsConstants.DISABLE_TEMP_DESTINATIONS_PROPERTY), CoreMatchers.<Object>is("true"));

        ImmutableEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointBuilder("endpoint2")
            .buildOutboundEndpoint();
        assertThat(endpoint2, is(notNullValue()));
        Filter filter2 = endpoint2.getFilter();
        assertThat(filter2, is(notNullValue()));
        assertThat(filter2, is(instanceOf(NotFilter.class)));
        Filter filter3 = ((NotFilter) filter2).getFilter();
        assertThat(filter3, is(notNullValue()));
        assertThat(filter3, is(instanceOf(JmsPropertyFilter.class)));

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

        assertThat(inboundEndpoint, is(notNullValue()));
        assertThat(inboundEndpoint.getProperties().size(), is(1));
        assertThat(inboundEndpoint.getProperty(JmsConstants.DURABLE_NAME_PROPERTY),
                   CoreMatchers.<Object>is("testCustomDurableName"));
    }

    @Test
    public void testCustomTransactions() throws EndpointException, InitialisationException
    {
        ImmutableEndpoint endpoint3 = muleContext.getRegistry()
            .lookupEndpointBuilder("endpoint3")
            .buildInboundEndpoint();
        assertThat(endpoint3, is(notNullValue()));
        TestTransactionFactory factory = (TestTransactionFactory) endpoint3.getTransactionConfig()
            .getFactory();
        assertThat(factory, is(notNullValue()));
        assertThat(factory.getValue(), is("foo"));
    }

    @Test
    public void testXaTransactions() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry()
            .lookupEndpointBuilder("endpoint4")
            .buildInboundEndpoint();
        assertThat(endpoint, is(notNullValue()));
        assertThat(endpoint.getTransactionConfig().getFactory(), is(instanceOf(XaTransactionFactory.class)));
        assertThat(endpoint.getTransactionConfig().getAction(), is(MuleTransactionConfig.ACTION_ALWAYS_JOIN));
    }

    @Test
    public void testJndiConnectorAttributes() throws Exception
    {
        JmsConnector connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsJndiConnector");
        assertThat("connection factory must be created only after connect so reconnection works when JNDI context is not yet available during start", connector.getConnectionFactory(), nullValue());
        connector.connect();
        assertThat(connector, is(notNullValue()));

        assertThat(connector.getJndiInitialFactory(), is("org.mule.transport.jms.test.JmsTestContextFactory"));
        assertThat(connector.getJndiProviderUrl(), is("jndi://test"));
        assertThat(connector.getConnectionFactoryJndiName(), is("jms/connectionFactory"));
        assertThat(connector.getConnectionFactory().getClass().getName(), is("org.mule.transport.jms.test.TestConnectionFactory"));
        assertThat(connector.isJndiDestinations(), is(true));
        assertThat(connector.isForceJndiDestinations(), is(true));
        assertThat(connector.getJndiProviderProperties().get("key"), CoreMatchers.<Object>is("value"));
        assertThat(connector.getConnectionFactoryProperties().get("customProperty"), CoreMatchers.<Object>is("customValue"));
        assertThat(((TestConnectionFactory) connector.getConnectionFactory()).getCustomProperty(),
                   CoreMatchers.<Object>is("customValue"));
    }

    @Test
    public void testActiveMqConnectorConfig()
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsActiveMqConnector");
        assertThat(c, is(notNullValue()));

        assertThat(c.getNumberOfConsumers(), is(1));

        c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsActiveMqConnectorXa");
        assertThat(c, is(notNullValue()));

        assertThat(c.getNumberOfConsumers(), is(1));
    }
}
