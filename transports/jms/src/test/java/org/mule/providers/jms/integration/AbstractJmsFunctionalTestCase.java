/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.integration;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;

/**
 * The main idea
 */
public abstract class AbstractJmsFunctionalTestCase extends FunctionalTestCase
{

    public static final String DEFAULT_BROKER_URL = "vm://localhost?broker.persistent=false&broker.useJmx=false";
    public static final String DEFAULT_INPUT_MESSAGE = "INPUT MESSAGE";
    public static final String DEFAULT_OUTPUT_MESSAGE = "OUTPUT MESSAGE";
    public static final String DEFAULT_INPUT_MQ_QUEUE_NAME = "in";
    public static final String DEFAULT_INPUT_MULE_QUEUE_NAME = "jms://" + DEFAULT_INPUT_MQ_QUEUE_NAME;
    public static final String DEFUALT_OUTPUT_MQ_QUEUE_NAME = "out";
    public static final String DEFUALT_OUTPUT_MULE_QUEUE_NAME = "jms://" + DEFUALT_OUTPUT_MQ_QUEUE_NAME;
    public static final long TIMEOUT = 5000;
    public static final long SMALL_TIMEOUT = 1000;
    public static final long LOCK_WAIT = 1000;
    public static final String CONNECTOR_NAME = "MuleMQConnector";
    private MuleClient client;

    protected void dispatchMessage() throws Exception
    {
        client.dispatch(DEFAULT_INPUT_MULE_QUEUE_NAME, DEFAULT_INPUT_MESSAGE, null);
    }

    protected UMOMessage recieveMessage() throws Exception
    {
        UMOMessage result = client.request(DEFUALT_OUTPUT_MULE_QUEUE_NAME, TIMEOUT);
        assertNotNull(result);
        assertNotNull(result.getPayload());
        assertNull(result.getExceptionPayload());
        assertEquals(DEFAULT_OUTPUT_MESSAGE, result.getPayload());
        return result;

    }

    public void runAsynchronousDispatching() throws Exception
    {
        dispatchMessage();
        recieveMessage();
        UMOMessage result = client.request(DEFUALT_OUTPUT_MULE_QUEUE_NAME, SMALL_TIMEOUT);
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

    interface Scenario
    {
        String getBrokerUrl();

        String getInputQueue();

        String getOutputQueue();

        int getAcknowledge();

        void send(Session session, MessageProducer producer) throws JMSException, SystemException, HeuristicMixedException, HeuristicRollbackException, RollbackException;

        Message receive(Session session, MessageConsumer consumer) throws JMSException, SystemException, HeuristicMixedException, HeuristicRollbackException, RollbackException;

        boolean isTransacted();
    }

    abstract class AbstractScenario implements Scenario
    {
        public String getBrokerUrl()
        {
            return DEFAULT_BROKER_URL;
        }

        public String getInputQueue()
        {
            return DEFAULT_INPUT_MQ_QUEUE_NAME;
        }

        public String getOutputQueue()
        {
            return DEFUALT_OUTPUT_MQ_QUEUE_NAME;
        }

        public int getAcknowledge()
        {
            return Session.AUTO_ACKNOWLEDGE;
        }

        public boolean isTransacted()
        {
            return false;
        }

        public void send(Session session, MessageProducer producer) throws JMSException, HeuristicMixedException, SystemException, HeuristicRollbackException, RollbackException
        {
            throw new UnsupportedOperationException();
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException, SystemException, HeuristicMixedException, HeuristicRollbackException, RollbackException
        {
            throw new UnsupportedOperationException();
        }
    }

    public void send(Scenario scenario) throws Exception
    {
        Connection connection = null;
        try
        {
            ConnectionFactory factory = new ActiveMQConnectionFactory(scenario.getBrokerUrl());
            connection = factory.createConnection();
            connection.start();
            Session session = null;
            try
            {
                session = connection.createSession(scenario.isTransacted(), scenario.getAcknowledge());
                ActiveMQQueue destination = new ActiveMQQueue(scenario.getInputQueue());
                MessageProducer producer = null;
                try
                {
                    producer = session.createProducer(destination);
                    scenario.send(session, producer);
                }
                catch (Exception e)
                {
                    throw e;
                }
                finally
                {
                    if (producer != null)
                    {
                        producer.close();
                    }
                }
            }
            catch (Exception e)
            {
                throw e;
            }
            finally
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }


    public Message receive(Scenario scenario) throws Exception
    {
        Connection connection = null;
        try
        {
            ConnectionFactory factory = new ActiveMQConnectionFactory(scenario.getBrokerUrl());
            connection = factory.createConnection();
            connection.start();
            Session session = null;
            try
            {
                session = connection.createSession(scenario.isTransacted(), scenario.getAcknowledge());
                ActiveMQQueue destination = new ActiveMQQueue(scenario.getOutputQueue());
                MessageConsumer consumer = null;
                try
                {
                    consumer = session.createConsumer(destination);
                    return scenario.receive(session, consumer);
                }
                catch (Exception e)
                {
                    throw e;
                }
                finally
                {
                    if (consumer != null)
                    {
                        consumer.close();
                    }
                }
            }
            catch (Exception e)
            {
                throw e;
            }
            finally
            {
                if (session != null)
                {
                    session.close();
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    Scenario scenarioRollback = new AbstractScenario()
    {
        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
            session.rollback();
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(TIMEOUT);
            assertNotNull(message);
            assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
            assertEquals(((TextMessage) message).getText(), DEFAULT_OUTPUT_MESSAGE);
            session.rollback();
            return message;
        }

        public boolean isTransacted()
        {
            return true;
        }
    };

    Scenario scenarioCommit = new AbstractScenario()
    {
        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
            session.commit();
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(TIMEOUT);
            assertNotNull(message);
            assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
            assertEquals(((TextMessage) message).getText(), DEFAULT_OUTPUT_MESSAGE);
            session.commit();
            return message;
        }

        public boolean isTransacted()
        {
            return true;
        }

    };

    Scenario scenarioNotReceive = new AbstractScenario()
    {
        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(SMALL_TIMEOUT);
            assertNull(message);
            return message;
        }

        public boolean isTransacted()
        {
            return false;
        }

    };

    Scenario scenarioNoTx = new AbstractScenario()
    {
        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(TIMEOUT);
            assertNotNull(message);
            assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
            assertEquals(((TextMessage) message).getText(), DEFAULT_OUTPUT_MESSAGE);
            return message;
        }
    };
    

}
