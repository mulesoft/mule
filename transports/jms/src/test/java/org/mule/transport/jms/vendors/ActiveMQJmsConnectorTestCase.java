/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.vendors;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static javax.jms.Session.DUPS_OK_ACKNOWLEDGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mule.transport.jms.activemq.ActiveMQJmsConnector.DEFAULT_BROKER_URL;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.jms.DefaultJmsTopicResolver;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.transport.jms.redelivery.JmsXRedeliveryHandler;
import org.mule.transport.jms.test.TestRedeliveryHandler;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;
import org.springframework.jms.connection.CachingConnectionFactory;

public class ActiveMQJmsConnectorTestCase extends FunctionalTestCase
{

    private static String USERNAME = "username";
    private static String PASSWORD = "password";

    @Override
    protected String getConfigFile()
    {
        return "integration/activemq-config.xml";
    }

    @Test
    public void testConfigurationDefaults() throws Exception
    {
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnector");
        assertThat(c, is(not(nullValue())));

        assertThat(c.isEagerConsumer(), equalTo(false));
        
        ConnectionFactory cf = c.getConnectionFactory();
        assertThat(cf, instanceOf(ActiveMQConnectionFactory.class));
        assertThat(DEFAULT_BROKER_URL, equalTo(((ActiveMQConnectionFactory) cf).getBrokerURL()));
        
        assertThat(c.getTopicResolver(), is(not(nullValue())));
        assertThat(c.getTopicResolver(), instanceOf(DefaultJmsTopicResolver.class));
    }
    
    @Test
    public void testDefaultActiveMqConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("activeMqJmsConnector");

        assertThat(c, is(not(nullValue())));
        assertThat(c, instanceOf(ActiveMQJmsConnector.class));
        
        assertThat(c.getConnectionFactory(), is(not(nullValue())));
        assertThat(c.getConnectionFactory(), instanceOf(ActiveMQConnectionFactory.class));
        assertThat(c.getAcknowledgementMode(), equalTo(AUTO_ACKNOWLEDGE));
        assertThat(c.getUsername(), is(nullValue()));
        assertThat(c.getPassword(), is(nullValue()));

        assertThat(c.getRedeliveryHandlerFactory(), is(not(nullValue())));
        assertThat(c.getRedeliveryHandlerFactory().create(), instanceOf(JmsXRedeliveryHandler.class));
        
        assertThat(c.isDurable(), equalTo(false));
        assertThat(c.isNoLocal(), equalTo(false));
        assertThat(c.isPersistentDelivery(), equalTo(false));
        assertThat(c.getMaxRedelivery(), equalTo(0));
        assertThat(c.getMaxQueuePrefetch(), equalTo(-1));
        assertThat(c.getMaximumRedeliveryDelay(), equalTo(-1));
        assertThat(c.getInitialRedeliveryDelay(), equalTo(-1));
        assertThat(c.getRedeliveryDelay(), equalTo(-1));
        assertThat(c.isCacheJmsSessions(), equalTo(true));
        assertThat(c.isEagerConsumer(), equalTo(false));

        assertThat(c.getSpecification(), equalTo("1.0.2b"));
    }
    
    @Test
    public void testCustomActiveMqConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("customActiveMqJmsConnector");

        assertThat(c, is(not(nullValue())));
        assertThat(c, instanceOf(ActiveMQJmsConnector.class));

        assertThat(c.getConnectionFactory(), is(not(nullValue())));
        assertThat(c.getConnectionFactory(), instanceOf(CachingConnectionFactory.class));
        assertThat(((CachingConnectionFactory) c.getConnectionFactory()).getTargetConnectionFactory(), instanceOf(ActiveMQConnectionFactory.class));
        assertThat(c.getAcknowledgementMode(), equalTo(DUPS_OK_ACKNOWLEDGE));
        assertThat(c.getUsername(), is(nullValue()));
        assertThat(c.getPassword(), is(nullValue()));

        assertThat(c.getRedeliveryHandlerFactory(), is(not(nullValue())));
        assertThat(c.getRedeliveryHandlerFactory().create(), instanceOf(TestRedeliveryHandler.class));

        assertThat("myClient", equalTo(c.getClientId()));
        assertThat(c.isDurable(), equalTo(true));
        assertThat(c.isNoLocal(), equalTo(true));
        assertThat(c.isPersistentDelivery(), equalTo(true));
        assertThat(c.getMaxRedelivery(), equalTo(5));
        assertThat(c.getMaxQueuePrefetch(), equalTo(5));
        assertThat(c.getMaximumRedeliveryDelay(), equalTo(2000));
        assertThat(c.getRedeliveryDelay(), equalTo(2000));
        assertThat(c.getInitialRedeliveryDelay(), equalTo(2000));
        assertThat(c.isCacheJmsSessions(), equalTo(true));
        assertThat(c.isEagerConsumer(), equalTo(false));

        assertThat(c.getSpecification(), equalTo("1.1")); // 1.0.2b is the default, should be changed in the config
    }

    /**
     * See MULE-8221
     */
    @Test
    public void testActiveMqConnectorWithUsernameAndPassword() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("activeMqJmsConnectorWithUsernameAndPassword");

        assertThat(c, instanceOf(ActiveMQJmsConnector.class));
        assertThat(c.isConnected(), equalTo(true));
        assertThat(c.isStarted(), equalTo(true));

        assertThat(c.getUsername(), equalTo((USERNAME)));
        assertThat(c.getPassword(), equalTo(PASSWORD));
        assertThat(c.isCacheJmsSessions(), equalTo(true));
        assertThat(c.getSpecification(), equalTo("1.1"));
    }

}
