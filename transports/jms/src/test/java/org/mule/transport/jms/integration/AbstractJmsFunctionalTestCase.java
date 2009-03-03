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
import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.MuleParameterized;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * The main idea, now runs as a parameterized JUnit 4 test
 */
@RunWith(MuleParameterized.class)
public abstract class AbstractJmsFunctionalTestCase extends FunctionalTestCase
{

    public static final String DEFAULT_INPUT_MESSAGE = "INPUT MESSAGE";
    public static final String DEFAULT_OUTPUT_MESSAGE = "OUTPUT MESSAGE";
    public static final String INBOUND_ENDPOINT_KEY = "inbound.destination";
    public static final String OUTBOUND_ENDPOINT_KEY = "outbound.destination";
    public static final String MIDDLE_ENDPOINT_KEY = "middle.destination";
    public static final String MIDDLE2_ENDPOINT_KEY = "middle2.destination";

    private MuleClient client = null;
    protected JmsVendorConfiguration jmsConfig = null;
    public static final String BROADCAST_TOPIC_ENDPOINT_KEY = "broadcast.topic.destination";

    protected Scenario scenarioNoTx;
    protected Scenario scenarioCommit;
    protected Scenario scenarioRollback;
    protected Scenario scenarioNotReceive;
    protected Scenario scenarioReceive;

    /**
     * Set the list of jms providers to test. The goal is to externalize this, i.e.
     * read the list from an xml file, use maven profiles to control it, etc.
     * 
     * @return
     */
    @Parameters
    public static Collection jmsProviderConfigs() throws Exception
    {
        JmsVendorConfiguration[][] configs = null;
        URL url = ClassUtils.getResource("jms-vendor-configs.txt", AbstractJmsFunctionalTestCase.class);
        if(url !=null)
        {
            List classes = IOUtils.readLines(url.openStream());
            configs = new JmsVendorConfiguration[1][classes.size()];
            int i=0;
            for (Iterator iterator = classes.iterator(); iterator.hasNext(); i++)
            {
                String cls = (String) iterator.next();
                configs[0][i] = (JmsVendorConfiguration)ClassUtils.instanciateClass(cls);

            }
        }
        return Arrays.asList(configs);
        //return Arrays.asList(new JmsVendorConfiguration[][]{{new ActiveMQJmsConfiguration()}});

    }

    /**
     * Since we are using JUnit 4, but the Mule Test Framework assumes JUnit 3, we
     * need to explicitly call the setUp and tearDown methods
     * 
     * @throws Exception
     */
    @Before
    public void before() throws Exception
    {
        super.setUp();
    }

    /**
     * Since we are using JUnit 4, but the Mule Test Framework assumes JUnit 3, we
     * need to explicitly call the setUp and tearDown methods
     * 
     * @throws Exception
     */
    @After
    public void after() throws Exception
    {
        super.tearDown();
    }

    public AbstractJmsFunctionalTestCase(JmsVendorConfiguration config)
    {
        setJmsConfig(config);
        scenarioNoTx = new NonTransactedScenario();
        scenarioCommit = new ScenarioCommit();
        scenarioRollback = new ScenarioRollback();
        scenarioNotReceive = new ScenarioNotReceive();
        scenarioReceive = new ScenarioReceive();
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties props = new Properties();
        // Inject endpoint names into the config
        props.put(INBOUND_ENDPOINT_KEY, getJmsConfig().getInboundEndpoint());
        props.put(OUTBOUND_ENDPOINT_KEY, getJmsConfig().getOutboundEndpoint());
        props.put(MIDDLE_ENDPOINT_KEY, getJmsConfig().getMiddleEndpoint());
        props.put(MIDDLE2_ENDPOINT_KEY, getJmsConfig().getMiddleEndpoint() + "2");

        props.put(BROADCAST_TOPIC_ENDPOINT_KEY, getJmsConfig().getTopicBroadcastEndpoint());
        props.put("protocol", getJmsConfig().getProtocol());

        Map p = getJmsConfig().getProperties();
        if (p != null)
        {
            props.putAll(p);
        }
        return props;
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        String resources = getConfigResources().substring(getConfigResources().lastIndexOf("/") + 1);
        resources = String.format("integration/%s/connector-%s,%s", getJmsConfig().getProviderName(),
            resources, getConfigResources());
        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder(resources);
        return builder;
    }

    public final JmsVendorConfiguration getJmsConfig()
    {
        if (jmsConfig == null)
        {
            jmsConfig = creatJmsConfig();
        }
        return jmsConfig;
    }

    public final void setJmsConfig(JmsVendorConfiguration jmsConfig)
    {
        this.jmsConfig = jmsConfig;
    }

    protected JmsVendorConfiguration creatJmsConfig()
    {
        // Overriding classes must override this or inject this object
        return null;
    }

    protected Connection getConnection(boolean topic, boolean xa) throws Exception
    {
        checkConfig();
        return getJmsConfig().getConnection(topic, xa);
    }

    protected String getInboundEndpoint()
    {
        checkConfig();
        return getJmsConfig().getInboundEndpoint();
    }

    protected String getOutboundEndpoint()
    {
        checkConfig();
        return getJmsConfig().getOutboundEndpoint();
    }

    protected String getInboundQueueName()
    {
        checkConfig();
        return getJmsConfig().getInboundDestinationName();
    }

    protected String getOutboundQueueName()
    {
        checkConfig();
        return getJmsConfig().getOutboundDestinationName();
    }

    /**
     * Timeout used when checking that a message is NOT present
     * 
     * @return
     */
    protected long getSmallTimeout()
    {
        checkConfig();
        return getJmsConfig().getSmallTimeout();

    }

    /**
     * The timeout used when waiting for a message to arrive
     * 
     * @return
     */
    protected long getTimeout()
    {
        checkConfig();
        return getJmsConfig().getTimeout();
    }

    protected void checkConfig()
    {
        if (getJmsConfig() == null)
        {
            throw new IllegalStateException("There must be a Jms Vendor config set on this test");
        }
    }

    protected void dispatchMessage() throws Exception
    {
        client.dispatch(getInboundEndpoint(), DEFAULT_INPUT_MESSAGE, null);
    }

    protected void dispatchMessage(Object payload) throws Exception
    {
        client.dispatch(getInboundEndpoint(), payload, null);
    }

    protected MuleMessage receiveMessage() throws Exception
    {
        MuleMessage result = client.request(getOutboundEndpoint(), getTimeout());
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertNull(result.getExceptionPayload());
        assertEquals(DEFAULT_OUTPUT_MESSAGE, result.getPayload());
        return result;

    }

    public void runAsynchronousDispatching() throws Exception
    {
        dispatchMessage();
        receiveMessage();
        MuleMessage result = client.request(getOutboundEndpoint(), getSmallTimeout());
        assertNull(result);
    }

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        client = new MuleClient();
    }

    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        client.dispose();
    }

    protected MuleClient getClient()
    {
        return client;
    }

    public void send(Scenario scenario) throws Exception
    {
        Connection connection = null;
        try
        {
            connection = getConnection(false, false);
            connection.start();
            Session session = null;
            try
            {
                session = connection.createSession(scenario.isTransacted(), scenario.getAcknowledge());
                Destination destination = createInputDestination(session, scenario);
                MessageProducer producer = null;
                try
                {
                    producer = session.createProducer(destination);
                    if (scenario.isPersistent())
                    {
                        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    }
                    scenario.send(session, producer);
                }
                finally
                {
                    if (producer != null)
                    {
                        producer.close();
                    }
                }
            }
            finally
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    /**
     * By default this will create a Queue, override to create a topic
     * 
     * @param session
     * @param scenario
     * @return
     * @throws JMSException
     */
    protected Destination createInputDestination(Session session, Scenario scenario) throws JMSException
    {
        return session.createQueue(scenario.getInputDestinationName());
    }

    /**
     * By default this will create a Queue, override to create a topic
     * 
     * @param session
     * @param scenario
     * @return
     * @throws JMSException
     */
    protected Destination createOutputDestination(Session session, Scenario scenario) throws JMSException
    {
        return session.createQueue(scenario.getOutputDestinationName());
    }

    /**/
    public Message receive(Scenario scenario) throws Exception
    {
        assertNotNull("scenario is null!", scenario);
        Connection connection = null;
        try
        {
            connection = getConnection(false, false);
            connection.start();
            Session session = null;
            try
            {
                session = connection.createSession(scenario.isTransacted(), scenario.getAcknowledge());
                Destination destination = createOutputDestination(session, scenario);
                MessageConsumer consumer = null;
                try
                {
                    consumer = session.createConsumer(destination);
                    return scenario.receive(session, consumer);
                }
                finally
                {
                    if (consumer != null)
                    {
                        consumer.close();
                    }
                }
            }
            finally
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////
    // Test Scenarios
    // /////////////////////////////////////////////////////////////////////////////////////////////////

    protected interface Scenario
    {

        boolean isPersistent();

        void setPersistent(boolean persistent);

        String getInputDestinationName();

        void setInputDestinationName(String inputQueue);

        String getOutputDestinationName();

        void setOutputDestinationName(String outputQueue);

        int getAcknowledge();

        void send(Session session, MessageProducer producer)
            throws JMSException, SystemException, HeuristicMixedException, HeuristicRollbackException,
            RollbackException;

        Message receive(Session session, MessageConsumer consumer)
            throws JMSException, SystemException, HeuristicMixedException, HeuristicRollbackException,
            RollbackException;

        boolean isTransacted();
    }

    protected abstract class AbstractScenario implements Scenario
    {

        private String inputQueue = getInboundQueueName();
        private String outputQueue = getOutboundQueueName();
        private boolean persistent = false;

        public boolean isPersistent()
        {
            return persistent;
        }

        public void setPersistent(boolean persistent)
        {
            this.persistent = persistent;
        }

        public String getInputDestinationName()
        {
            return inputQueue;
        }

        public String getOutputDestinationName()
        {
            return outputQueue;
        }

        public void setInputDestinationName(String inputQueue)
        {
            this.inputQueue = inputQueue;
        }

        public void setOutputDestinationName(String outputQueue)
        {
            this.outputQueue = outputQueue;
        }

        public int getAcknowledge()
        {
            return Session.AUTO_ACKNOWLEDGE;
        }

        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
            applyTransaction(session);
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(getTimeout());
            assertNotNull(message);
            assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
            assertEquals(DEFAULT_OUTPUT_MESSAGE, ((TextMessage) message).getText());
            applyTransaction(session);
            return message;
        }

        abstract protected void applyTransaction(Session session) throws JMSException;
    }

    protected class NonTransactedScenario extends AbstractScenario
    {

        public boolean isTransacted()
        {
            return false;
        }

        protected void applyTransaction(Session session) throws JMSException
        {
            // do nothing
        }
    }

    protected class ScenarioCommit extends AbstractScenario
    {

        public boolean isTransacted()
        {
            return true;
        }

        protected void applyTransaction(Session session) throws JMSException
        {
            session.commit();
        }
    }

    protected class ScenarioRollback extends AbstractScenario
    {

        public boolean isTransacted()
        {
            return true;
        }

        protected void applyTransaction(Session session) throws JMSException
        {
            session.rollback();
        }
    }

    protected class ScenarioNotReceive extends NonTransactedScenario
    {

        @Override
        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(getSmallTimeout());
            assertNull(message);
            return message;
        }
    }

    protected class ScenarioReceive extends NonTransactedScenario
    {

        @Override
        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(getTimeout());
            assertNotNull(message);
            return message;
        }
    }
}
