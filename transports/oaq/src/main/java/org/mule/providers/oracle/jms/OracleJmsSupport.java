/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import org.mule.providers.jms.Jms102bSupport;
import org.mule.providers.jms.JmsConnector;
import org.mule.util.ClassUtils;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.naming.Context;

import oracle.jms.AQjmsSession;

/**
 * Extends the standard Mule JMS Provider with functionality specific to Oracle's JMS
 * implementation based on Advanced Queueing (Oracle AQ). Oracle 9i supports the JMS
 * 1.0.2b specification while Oracle 10g supports JMS 1.1
 * 
 * @see OracleJmsConnector
 * @see org.mule.providers.jms.Jms102bSupport
 * @see <a href="http://otn.oracle.com/pls/db102/">Streams Advanced Queuing</a>
 * @see <a
 *      href="http://www.lc.leidenuniv.nl/awcourse/oracle/appdev.920/a96587/jcreate.htm#103729">Oracle9i
 *      J2EE Compliance</a>
 */
public class OracleJmsSupport extends Jms102bSupport
{

    /**
     * Since we can't access the endpoint's properties directly from the scope of
     * this class, we save a copy of the properties in this local variable for easy
     * reference.
     */
    private Map endpointProperties;

    public OracleJmsSupport(JmsConnector connector,
                            Context context,
                            boolean jndiDestinations,
                            boolean forceJndiDestinations)
    {
        super(connector, context, jndiDestinations, forceJndiDestinations);
    }

    /**
     * Returns an OracleJmsConnection to masquerade the fact that there might be
     * several javax.jms.Connections open (one per session).
     * 
     * @see OracleJmsConnection
     */
    public Connection createConnection(ConnectionFactory connectionFactory) throws JMSException
    {
        return createConnection(connectionFactory, /* username */null, /* password */null);
    }

    /**
     * Returns an OracleJmsConnection to masquerade the fact that there might be
     * several javax.jms.Connections open (one per session).
     * 
     * @see OracleJmsConnection
     */
    public javax.jms.Connection createConnection(ConnectionFactory connectionFactory,
                                                 String username,
                                                 String password) throws JMSException
    {
        return new OracleJmsConnection((OracleJmsConnector) connector);
    }

    /**
     * In order to receive messages from a queue whose payload is an ADT (Oracle
     * Advanced Data Type), we must pass the payload factory as a parameter when
     * creating the receiver/subscriber.
     * 
     * @see OracleJmsConnector#PAYLOADFACTORY_PROPERTY
     */
    public MessageConsumer createConsumer(Session session,
                                          Destination destination,
                                          String messageSelector,
                                          boolean noLocal,
                                          String durableName,
                                          boolean topic) throws JMSException
    {

        Object payloadFactory = getPayloadFactory();
        if (payloadFactory == null)
        {
            return super.createConsumer(session, destination, messageSelector, noLocal, durableName, topic);
        }
        else
        {
            if (topic && session instanceof TopicSession)
            {
                if (durableName == null)
                {
                    return ((AQjmsSession) session).createSubscriber((Topic) destination, messageSelector,
                        noLocal);
                }
                else
                {
                    return ((AQjmsSession) session).createDurableSubscriber((Topic) destination,
                        durableName, messageSelector, noLocal, payloadFactory);
                }
            }
            else if (session instanceof QueueSession)
            {
                if (messageSelector != null)
                {
                    return ((AQjmsSession) session).createReceiver((Queue) destination, messageSelector,
                        payloadFactory);
                }
                else
                {
                    return ((AQjmsSession) session).createReceiver((Queue) destination, payloadFactory);
                }
            }
            else
            {
                throw new IllegalArgumentException("Session and domain type do not match");
            }
        }
    }

    /**
     * The standard Oracle JMS classes ({@code oracle.jms}) do not support dynamic
     * (i.e., run-time) creation of queues. This is only possible through the
     * (non-standard) administrative classes ({@code oracle.AQ}). Therefore this
     * method, which calls {@code AQjmsSession.createQueue(name)} or
     * {@code AQjmsSession.createTopic(name)} will inevitably fail. The failure
     * <i>should</i> produce a {@code JMSException} but for some reason it doesn't
     * (maybe an Oracle bug) and just returns null. In this case, we generate the
     * appropriate exception.
     */
    public Destination createDestination(Session session, String name, boolean topic) throws JMSException
    {
        Destination dest = super.createDestination(session, name, topic);
        if (dest != null)
        {
            return dest;
        }
        else
        {
            throw new JMSException(
                    "Oracle JMS was unable to bind to the "
                            + (topic ? "topic" : "queue")
                            + ": "
                            + name
                            + " but gives no exception nor error message to explain why (that's what you get for using proprietary software...)");
        }
    }

    /**
     * The standard Oracle JMS classes ({@code oracle.jms}) do not support dynamic
     * (i.e., run-time) creation of queues. This is only possible through the
     * (non-standard) administrative classes ({@code oracle.AQ}). Therefore this
     * method, which calls {@code AQjmsSession.createQueue(name)} or
     * {@code AQjmsSession.createTopic(name)} will inevitably fail. The failure
     * <i>should</i> produce a {@code JMSException} but for some reason it doesn't
     * (maybe an Oracle bug) and just returns null. In this case, we generate the
     * appropriate exception.
     */
    public Destination createTemporaryDestination(Session session, boolean topic) throws JMSException
    {
        Destination dest = super.createTemporaryDestination(session, topic);
        if (dest != null)
        {
            return dest;
        }
        else
        {
            throw new JMSException("Unable to create temporary " + (topic ? "topic" : "queue"));
        }
    }

    /**
     * Get the payload factory class, if defined, from the connector or endpoint's
     * properties.
     * 
     * @see OracleJmsConnector#PAYLOADFACTORY_PROPERTY
     */
    public Object getPayloadFactory() throws JMSException
    {

        // Get the global property set on the connector, if any.
        String payloadFactoryClass = ((OracleJmsConnector) connector).getPayloadFactory();

        // If the property has been set for this endpoint, it overrides the global
        // setting.
        if ((endpointProperties != null)
            && (endpointProperties.get(OracleJmsConnector.PAYLOADFACTORY_PROPERTY) != null))
        {
            payloadFactoryClass = (String) endpointProperties.get(OracleJmsConnector.PAYLOADFACTORY_PROPERTY);
        }

        Object payloadFactory = null;
        if (payloadFactoryClass != null)
        {
            Throwable ex = null;
            try
            {
                // TODO ClassUtils call is more suitable here
                payloadFactory = ClassUtils.loadClass(payloadFactoryClass, this.getClass()).newInstance();
            }
            catch (ClassNotFoundException e)
            {
                ex = e;
            }
            catch (IllegalAccessException e)
            {
                ex = e;
            }
            catch (InstantiationException e)
            {
                ex = e;
            }
            if (ex != null)
            {
                throw new JMSException("Unable to instantiate payload factory class " + payloadFactoryClass
                        + ": " + ex.getMessage());
            }
        }
        return payloadFactory;
    }

    public Map getEndpointProperties()
    {
        return endpointProperties;
    }

    public void setEndpointProperties(Map endpointProperties)
    {
        this.endpointProperties = endpointProperties;
    }

}
