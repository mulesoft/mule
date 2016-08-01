/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.Serializable;
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
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base class for all integration tests that are part of the JMS integration test suite.  This is
 * a suite that can be run on multiple JMS providers since all configuration for the provider is abstracted into
 * a single class which implements {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration}.  The implementation
 * of this class is loaded by looking for the classname in a properties file called 'jms-vendor-configs.txt' in the root
 * classpath.
 * <p/>
 * This test case provides a number of support methods for testing Jms providers with Mule.  This implementation is based
 * around the concept of scenarios.  Scenarios define an action or set of actions and are represented as implementations
 * of {@link org.mule.compatibility.transport.jms.integration.AbstractJmsFunctionalTestCase.Scenario}.  Scenarios can be combined to create
 * a test.  The default scenarios are usually sufficient to create a test.  These are:
 * {@link org.mule.compatibility.transport.jms.integration.AbstractJmsFunctionalTestCase.ScenarioReceive}
 * {@link org.mule.compatibility.transport.jms.integration.AbstractJmsFunctionalTestCase.ScenarioNotReceive}
 * {@link org.mule.compatibility.transport.jms.integration.AbstractJmsFunctionalTestCase.ScenarioCommit}
 * {@link org.mule.compatibility.transport.jms.integration.AbstractJmsFunctionalTestCase.ScenarioRollback}
 * {@link org.mule.compatibility.transport.jms.integration.AbstractJmsFunctionalTestCase.NonTransactedScenario}
 * <p/>
 * This object will also add properties to the registry that can be accessed within XML config files using placeholders.
 * The following properties are made available -
 * <ul>
 * <li>${inbound.destination} - the URI of the inbound destination (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
 * <li>${outbound.destination} - the URI of the outbound destination (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
 * <li>${middle.destination} - the URI of the middle destination (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
 * <li>${middle2.destination} - the URI of a second middle destination 'middle2'.</li>
 * <li>${middle3.destination} - the URI of a third middle destination 'middle3'.</li>
 * <li>${broadcast.destination} - the URI of the broadcast topic (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
 * <li>${protocol} - the protocol of the current messaging connector (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
 * </ul>
 * <p/>
 * For each integration test there are 2 configuration files. One is provided by the JMS integration suite and defines the
 * event flow for the test. The other is a vendor-specific config file that defines the connectors and possibly endpoints and
 * transformers for the Jms connector being tested. These configurations are known as 'connector' files, they share the same
 * file name as the generic configuration file prepended with 'connector-'.  The location of these files must be
 * <p/>
 * <code>integration/&lt;provider_name>/connector-&lt;event_flow_config_name></code>
 * <p/>
 * The 'provider_name' is obtained from the {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation.
 * <p/>
 * In order to know what objects to define in the 'connector-' files you must copy the connector files from the ActiveMQ (default)
 * test suite and configure the objects to match the configuration in the ActiveMQ tests.  Note that the object names must
 * be consistently the same for things to work.
 */
public abstract class AbstractJmsFunctionalTestCase extends FunctionalTestCase
{

    public static final String DEFAULT_INPUT_MESSAGE = "INPUT MESSAGE";
    public static final String DEFAULT_OUTPUT_MESSAGE = "OUTPUT MESSAGE";

    public static final String INBOUND_ENDPOINT_KEY = "inbound.destination";
    public static final String OUTBOUND_ENDPOINT_KEY = "outbound.destination";

    public static final String MIDDLE_ENDPOINT_KEY = "middle.destination";
    public static final String MIDDLE2_ENDPOINT_KEY = "middle2.destination";
    public static final String MIDDLE3_ENDPOINT_KEY = "middle3.destination";
    public static final String BROADCAST_TOPIC_ENDPOINT_KEY = "broadcast.topic.destination";

    protected static final Logger log = LoggerFactory.getLogger("MULE_TESTS");

    protected JmsVendorConfiguration jmsConfig = null;
    protected Scenario scenarioNoTx;

    protected Scenario scenarioCommit;
    protected Scenario scenarioRollback;
    protected Scenario scenarioNotReceive;
    protected Scenario scenarioReceive;
    protected boolean purgeQueuesOnPreSetUp = true;
    protected boolean purgeQueuesOnTearDown = true;

    private MuleClient client = null;

    /**
     * This test case is refactored to support multiple JMS providers.
     */
    private boolean multipleProviders = true;

    /**
     * Finds the {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} instances to test with by looking
     * in a file called "jms-vendor-configs.txt" which contains one or more fuly qualified classnames of
     * {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} instances to load.
     *
     * @return a collection of {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} instance to test
     * against.
     *
     * @throws Exception if the 'jms-vendor-configs.txt' cannot be loaded or the classes defined within that file
     * are not on the classpath
     *
     * TODO this method can return more than one provider, but our test class can only handle one at a time
     * IMPORTANT: Only set one class in 'jms-vendor-configs.txt'
     */
    public static Collection<?> jmsProviderConfigs()
    {
        JmsVendorConfiguration[][] configs;
        URL url = ClassUtils.getResource("jms-vendor-configs.txt", AbstractJmsFunctionalTestCase.class);

        if (url == null)
        {
            fail("Please specify the org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration " +
                 "implementation to use in jms-vendor-configs.txt on classpaath.");
            return CollectionUtils.EMPTY_COLLECTION;
        }

        if (log.isInfoEnabled())
        {
            log.info("Parameterized test using: " + url);
        }

        try
        {
            List<?> classes = IOUtils.readLines(url.openStream());
            configs = new JmsVendorConfiguration[1][classes.size()];
            int i = 0;
            for (Iterator<?> iterator = classes.iterator(); iterator.hasNext(); i++)
            {
                String cls = (String) iterator.next();
                configs[0][i] = (JmsVendorConfiguration) ClassUtils.instanciateClass(cls, ClassUtils.NO_ARGS);
            }
            return Arrays.asList(configs);
        }
        catch (Exception e)
        {
            fail("Please specify the org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration " +
                 "implementation to use in jms-vendor-configs.txt on classpath: " + e.getMessage());
            return CollectionUtils.EMPTY_COLLECTION;
        }
    }

    public AbstractJmsFunctionalTestCase()
    {
        // TODO jmsProviderConfigs() can return more than one provider, but our test class can only handle one at a time
        this(((JmsVendorConfiguration[]) jmsProviderConfigs().iterator().next())[0]);
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

    /**
     * Adds the following properties to the registry so that the Xml configuration files can reference them.
     * <p/>
     * <ul>
     * <li>${inbound.destination} - the URI of the inbound destination (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
     * <li>${outbound.destination} - the URI of the outbound destination (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
     * <li>${middle.destination} - the URI of the middle destination (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
     * <li>${middle2.destination} - the URI of a second middle destination 'middle2'.</li>
     * <li>${middle3.destination} - the URI of a third middle destination 'middle3'.</li>
     * <li>${broadcast.destination} - the URI of the broadcast topic (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
     * <li>${protocol} - the protocol of the current messaging connector (retrieved from an {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implementation)</li>
     * </ul>
     */
    @Override
    protected Properties getStartUpProperties()
    {
        Properties props = new Properties();
        // Inject endpoint names into the config
        props.put(INBOUND_ENDPOINT_KEY, getJmsConfig().getInboundEndpoint());
        props.put(OUTBOUND_ENDPOINT_KEY, getJmsConfig().getOutboundEndpoint());
        props.put(MIDDLE_ENDPOINT_KEY, getJmsConfig().getMiddleEndpoint());
        props.put(MIDDLE2_ENDPOINT_KEY, getJmsConfig().getMiddleEndpoint() + "2");
        props.put(MIDDLE3_ENDPOINT_KEY, getJmsConfig().getMiddleEndpoint() + "3");

        props.put(BROADCAST_TOPIC_ENDPOINT_KEY, getJmsConfig().getTopicBroadcastEndpoint());
        props.put("protocol", getJmsConfig().getProtocol());

        Map<?, ?> p = getJmsConfig().getProperties();
        if (p != null)
        {
            props.putAll(p);
        }
        return props;
    }

    /**
     * This creates a {@link org.mule.runtime.config.spring.SpringXmlConfigurationBuilder} as expected but also figures out
     * which 'connector' configuration file to load with the event flow configuration (obtained from the overriding
     * class which implements {@link #getConfigFile()}).
     *
     * @return The config builder used to create the Mule instance for this test
     * @throws Exception
     */
    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        if (multipleProviders)
        {
            final String configFile = getConfigFile();
            // multiple configs arent' supported by this mechanism, validate and fail if needed
            if (StringUtils.splitAndTrim(configFile, ",; ").length > 1)
            {
                throw new IllegalArgumentException("Parameterized tests don't support multiple " +
                                                   "config files as input: " + configFile);
            }

            String resources = configFile.substring(configFile.lastIndexOf("/") + 1);
            resources = String.format("integration/%s/connector-%s",
                                      getJmsConfig().getName(), resources);

            String[] configFiles = new String[] { resources, configFile };
            SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder(configFiles);
            return builder;
        }
        else
        {
            return super.getBuilder();
        }
    }

    /**
     * Returns the {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implemetation to be used
     * with this test
     *
     * @return settings for this test
     */
    public final JmsVendorConfiguration getJmsConfig()
    {
        if (jmsConfig == null)
        {
            jmsConfig = createJmsConfig();
        }
        return jmsConfig;
    }

    /**
     * Sets the {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} implemetation to be used
     * with this test
     *
     * @param jmsConfig the settings for this test
     */
    public final void setJmsConfig(JmsVendorConfiguration jmsConfig)
    {
        this.jmsConfig = jmsConfig;
    }

    /**
     * Overriding classes must override this or inject this object. It is responsible for creating the
     * {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} instance to be used by this test.
     *
     * @return the settings for this test
     */
    protected JmsVendorConfiguration createJmsConfig()
    {
        // Overriding classes must override this or inject this object
        return null;
    }

    /**
     * Create a connection factory for the Jms profider being tested.  This calls
     * through to {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration#getConnection(boolean, boolean)}
     *
     * @param topic whether to use a topic or queue connection factory, for 1.1
     *              implementations this proerty can be ignored
     * @param xa    whether to create an XA connection factory
     * @return a new JMS connection
     */
    protected final Connection getConnection(boolean topic, boolean xa) throws Exception
    {
        checkConfig();
        return getJmsConfig().getConnection(topic, xa);
    }

    /**
     * Returns the {@link #getInboundQueueName()} in the form of an endpoint URI i.e.
     * jms://in.
     * <p/>
     * This calls through to {@link JmsVendorConfiguration#getInboundEndpoint()}
     *
     * @return the Inbound JMS endpoint
     */
    protected final String getInboundEndpoint()
    {
        checkConfig();
        return getJmsConfig().getInboundEndpoint();
    }

    /**
     * Returns the {@link #getOutboundQueueName()} in the form of an endpoint URI i.e.
     * jms://out.
     * <p/>
     * This calls through to {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration#getOutboundEndpoint()}
     *
     * @return the Outbound JMS endpoint
     */
    protected final String getOutboundEndpoint()
    {
        checkConfig();
        return getJmsConfig().getOutboundEndpoint();
    }

    /**
     * The test inbound queue name.  For consistency this should always be 'in'. Note that you need to make
     * sure that this queue is available in the the JMS provider being tested.
     * <p/>
     * This calls through to {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration#getInboundDestinationName()}
     *
     * @return The test inbound destination name
     */
    protected final String getInboundQueueName()
    {
        checkConfig();
        return getJmsConfig().getInboundDestinationName();
    }

    /**
     * The test dead letter queue name.  For consistency this should always be 'dlq'. Note that you need to make
     * sure that this queue is available in the the JMS provider being tested.
     * <p/>
     * This calls through to {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration#getDeadLetterDestinationName()}
     *
     * @return The test inbound destination name
     */
    protected final String getDeadLetterQueueName()
    {
        checkConfig();
        return getJmsConfig().getDeadLetterDestinationName();
    }

    /**
     * The test outbound queue name.  For consistency this should always be 'out'. Note that you need to make
     * sure that this queue is available in the the JMS provider being tested.
     * <p/>
     * This calls through to {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration#getOutboundDestinationName()}
     *
     * @return The test outbound destination name
     */
    protected final String getOutboundQueueName()
    {
        checkConfig();
        return getJmsConfig().getOutboundDestinationName();
    }

    /**
     * Timeout in milliseconds used when checking that a message is NOT present. This is usually 1000-2000ms.
     * It is customizable so that slow connections i.e. over a wAN can be accounted for.
     * <p/>
     * This calls through to {@link JmsVendorConfiguration#getSmallTimeout()}
     *
     * @return timeout in milliseconds used when checking that a message is NOT present
     */
    protected final long getSmallTimeout()
    {
        checkConfig();
        return getJmsConfig().getSmallTimeout();

    }

    /**
     * The timeout in milliseconds used when waiting for a message to arrive. This is usually 3000-5000ms.
     * However, it is customizable so that slow connections i.e. over a wAN can be accounted for.
     * <p/>
     * This calls through to {@link JmsVendorConfiguration#getTimeout()}
     *
     * @return The timeout used when waiting for a message to arrive
     */
    protected final long getTimeout()
    {
        checkConfig();
        return getJmsConfig().getTimeout();
    }

    /**
     * Ensures that the {@link org.mule.compatibility.transport.jms.integration.JmsVendorConfiguration} instance is not null
     * if it is an {@link IllegalStateException} will be thrown
     */
    protected void checkConfig()
    {
        if (getJmsConfig() == null)
        {
            throw new IllegalStateException("There must be a Jms Vendor config set on this test");
        }
    }

    protected void dispatchMessage() throws Exception
    {
        dispatchMessage(DEFAULT_INPUT_MESSAGE);
    }

    protected void dispatchMessage(Object payload) throws Exception
    {
        dispatchMessage(payload, null);
    }

    protected void dispatchMessage(Object payload, Map<String, Serializable> props) throws Exception
    {
        client.dispatch(getInboundEndpoint(), payload, props);
    }

    protected MuleMessage receiveMessage() throws Exception
    {
        return receiveMessage(DEFAULT_OUTPUT_MESSAGE);
    }

    protected MuleMessage receiveMessage(Object expected) throws Exception
    {
        MuleMessage result = client.request(getOutboundEndpoint(), getTimeout());
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertNull(result.getExceptionPayload());
        assertEquals(expected, result.getPayload());
        return result;
    }

    protected MuleMessage receiveMessage(byte[] expected) throws Exception
    {
        MuleMessage result = client.request(getOutboundEndpoint(), getTimeout());
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertNull(result.getExceptionPayload());
        byte[] bytes = getPayloadAsBytes(result);
        assertEquals("Wrong number of bytes", expected.length, bytes.length);
        for (int i=0; i < expected.length; ++i)
        {
            assertEquals("Byte #" + i + " does not match", expected[i], bytes[i]);
        }
        return result;
    }

    public void runAsynchronousDispatching() throws Exception
    {
        dispatchMessage();
        receiveMessage();
        MuleMessage result = client.request(getOutboundEndpoint(), getSmallTimeout());
        assertNull(result);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        if (purgeQueuesOnPreSetUp)
        {
            purge(getInboundQueueName());
            purge(getOutboundQueueName());
            // TODO DZ: get all of the queue/topic names from the Mule config and just purge those
        }

        client = muleContext.getClient();
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(tx);
            log.warn("Transaction was active when this test began");
        }
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (purgeQueuesOnTearDown)
        {
            purge(getInboundQueueName());
            purge(getOutboundQueueName());
            purgeTopics();
        }

        super.doTearDown();
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(tx);
            log.warn("Transaction was active when this test ended");
        }
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
     */
    protected Destination createInputDestination(Session session, Scenario scenario) throws JMSException
    {
        return session.createQueue(scenario.getInputDestinationName());
    }

    /**
     * By default this will create a Queue, override to create a topic
     */
    protected Destination createOutputDestination(Session session, Scenario scenario) throws JMSException
    {
        return session.createQueue(scenario.getOutputDestinationName());
    }

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
                try
                {
                    connection.close();
                }
                catch (JMSException e)
                {
                    log.warn("Failed to close jms connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Purge destinations for clean test setup. Especially applicable to WMQ tests, as messages from
     * other tests may still exist from other tests' runs.
     * <p/>
     * Well-behaving tests should drain both inbound and outbound destinations, as well as any intermediary ones.
     * @param destination destination name without any protocol specifics
     */
    protected void purge(final String destination) throws JMSException
    {
        Connection c = null;
        Session s = null;
        try
        {
            log.debug("purging queue : " + destination);
            c = getConnection(false, false);
            assertNotNull(c);
            c.start();

            s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination d = s.createQueue(destination);
            MessageConsumer consumer = s.createConsumer(d);

            while (consumer.receiveNoWait() != null)
            {
                log.debug("Destination " + destination + " isn't empty, draining it");
            }
        }
        catch (Exception e)
        {
            log.error("unable to purge : " + destination);
        }
        finally
        {
            if (c != null)
            {
                c.stop();
                if (s != null)
                {
                    s.close();
                }
                try
                {
                    c.close();
                }
                catch (JMSException e)
                {
                    log.warn("Failed to close jms connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Purge all of the topics which are created during testing
     * TODO DZ: we should be getting this list dynamically, and only calling them for the topic tests
     * @throws Exception
     */
    protected void purgeTopics() throws Exception
    {
        String destination = "broadcast";
        purgeTopic(destination, "Client1");
        purgeTopic(destination, "Client2");
        purgeTopic(destination, "mule.JmsConnectorC1.broadcast");
        purgeTopic(destination, "mule.JmsConnectorC2.broadcast");
    }

    /**
     * Clear the specified topic
     */
    protected void purgeTopic(String destination, String topic) throws Exception
    {
        Connection c = null;
        Session s = null;

        try
        {
            log.debug("purging topic : " + topic);
            c = getConnection(true, false);
            if (c == null)
            {
                log.debug("could not create a connection to topic : " + destination);
            }

            c.start();
            s = ((TopicConnection) c).createTopicSession(true, Session.SESSION_TRANSACTED);

            log.debug("created topic session");
            Topic dest = s.createTopic(destination);
            log.debug("created topic destination");

            MessageConsumer consumer = null;

            try
            {
                consumer = s.createDurableSubscriber(dest, topic);
                log.debug("created consumer");
                while (consumer.receiveNoWait() != null)
                {
                    log.debug("Topic " + topic + " isn't empty, draining it");
                }
                log.debug("topic should be empty");
                consumer.close();
                s.unsubscribe(topic);
            }
            catch (JMSException e)
            {
                log.debug("could not unsubscribe : " + topic);
            }
        }

        finally
        {
            if (c != null)
            {
                c.stop();
                if (s != null)
                {
                    s.close();
                }
                try
                {
                    c.close();
                }
                catch (JMSException e)
                {
                    log.warn("Failed to close jms connection: " + e.getMessage());
                }
            }
        }
        log.debug("completed draining topic :" + topic);
    }

    public boolean isMultipleProviders()
    {
        return multipleProviders;
    }

    public void setMultipleProviders(boolean multipleProviders)
    {
        this.multipleProviders = multipleProviders;
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

        @Override
        public boolean isPersistent()
        {
            return persistent;
        }

        @Override
        public void setPersistent(boolean persistent)
        {
            this.persistent = persistent;
        }

        @Override
        public String getInputDestinationName()
        {
            return inputQueue;
        }

        @Override
        public String getOutputDestinationName()
        {
            return outputQueue;
        }

        @Override
        public void setInputDestinationName(String inputQueue)
        {
            this.inputQueue = inputQueue;
        }

        @Override
        public void setOutputDestinationName(String outputQueue)
        {
            this.outputQueue = outputQueue;
        }

        @Override
        public int getAcknowledge()
        {
            return Session.AUTO_ACKNOWLEDGE;
        }

        @Override
        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
            applyTransaction(session);
        }

        @Override
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
        @Override
        public boolean isTransacted()
        {
            return false;
        }

        @Override
        protected void applyTransaction(Session session) throws JMSException
        {
            // do nothing
        }
    }

    protected class ScenarioCommit extends AbstractScenario
    {
        @Override
        public boolean isTransacted()
        {
            return true;
        }

        @Override
        protected void applyTransaction(Session session) throws JMSException
        {
            session.commit();
        }
    }

    protected class ScenarioRollback extends AbstractScenario
    {
        @Override
        public boolean isTransacted()
        {
            return true;
        }

        @Override
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
