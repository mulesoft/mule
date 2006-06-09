/*
 * $Id$
 * -----------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.providers.jms;

import org.apache.commons.collections.MapUtils;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.ConnectException;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Registers a single Jms MessageListener for an endpoint
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 *
 */
public class SingleJmsMessageReceiver extends AbstractMessageReceiver implements MessageListener
{

    protected JmsConnector connector;
    protected RedeliveryHandler redeliveryHandler;
    protected MessageConsumer consumer;
    protected Session session;
    protected boolean startOnConnect = false;


    public SingleJmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException
    {
        super(connector, component, endpoint);
        this.connector = (JmsConnector) connector;

        try {
            redeliveryHandler = this.connector.createRedeliveryHandler();
            redeliveryHandler.setConnector(this.connector);
        } catch (Exception e) {
            throw new InitialisationException(e, this);
        }
    }

    public void doConnect() throws Exception
    {
        createConsumer();
        if(startOnConnect) {
            doStart();
        }
    }

    public void doDisconnect() throws Exception
    {
        closeConsumer();
    }

    public void onMessage(Message message)
    {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Message received it is of type: " + message.getClass().getName());
                if (message.getJMSDestination() != null) {
                    logger.debug("Message received on " + message.getJMSDestination() + " ("
                            + message.getJMSDestination().getClass().getName() + ")");
                } else {
                    logger.debug("Message received on unknown destination");
                }
                logger.debug("Message CorrelationId is: " + message.getJMSCorrelationID());
                logger.debug("Jms Message Id is: " + message.getJMSMessageID());
            }

            if (message.getJMSRedelivered()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Message with correlationId: " + message.getJMSCorrelationID()
                            + " is redelivered. handing off to Exception Handler");
                }
                redeliveryHandler.handleRedelivery(message);
            }

            UMOMessageAdapter adapter = connector.getMessageAdapter(message);
            routeMessage(new MuleMessage(adapter));
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void doStart() throws UMOException {
        try {
            //We ned to register the listener when start is called in order to only start receiving messages after
            //start/
            //If the consumer is null it means that the connection strategy is being run in a separate thread
            //And hasn't managed to connect yet.
            if(consumer==null)  {
                startOnConnect=true;
            } else {
                startOnConnect=false;
                consumer.setMessageListener(this);
            }
        } catch (JMSException e) {
            throw new LifecycleException(e, this);
        }
    }

    public void doStop() throws UMOException {
        try {
            if(consumer!=null) {
                consumer.setMessageListener(null);
            }
        } catch (JMSException e) {
            throw new LifecycleException(e, this);
        }
    }

    protected void closeConsumer()
    {
        connector.closeQuietly(consumer);
        consumer = null;
        connector.closeQuietly(session);
        session = null;
    }

    /**
     * Create a consumer for the jms destination
     *
     * @throws Exception
     */
    protected void createConsumer() throws Exception
    {
        try {
            JmsSupport jmsSupport = this.connector.getJmsSupport();
            // Create session if none exists
            if (session == null) {
                session = this.connector.getSession(endpoint);
            }

            // Create destination
            String resourceInfo = endpoint.getEndpointURI().getResourceInfo();
            boolean topic = (resourceInfo != null && JmsConstants.TOPIC_PROPERTY.equalsIgnoreCase(resourceInfo));

            //todo MULE20 remove resource Info support
            if(!topic) {
                topic = MapUtils.getBooleanValue(endpoint.getProperties(),
                        JmsConstants.TOPIC_PROPERTY, false);
            }

            Destination dest = jmsSupport.createDestination(session, endpoint.getEndpointURI().getAddress(), topic);

            // Extract jms selector
            String selector = null;
            if (endpoint.getFilter() != null && endpoint.getFilter() instanceof JmsSelectorFilter) {
                selector = ((JmsSelectorFilter) endpoint.getFilter()).getExpression();
            } else if (endpoint.getProperties() != null) {
                // still allow the selector to be set as a property on the endpoint
                // to be backward compatable
                selector = (String) endpoint.getProperties().get(JmsConstants.JMS_SELECTOR_PROPERTY);
            }
            String tempDurable = (String) endpoint.getProperties().get(JmsConstants.DURABLE_PROPERTY);
            boolean durable = connector.isDurable();
            if (tempDurable != null) {
                durable = Boolean.valueOf(tempDurable).booleanValue();
            }

            // Get the durable subscriber name if there is one
            String durableName = (String) endpoint.getProperties().get(JmsConstants.DURABLE_NAME_PROPERTY);
            if (durableName == null && durable && dest instanceof Topic) {
                durableName = "mule." + connector.getName() + "." + endpoint.getEndpointURI().getAddress();
                logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: "
                        + durableName);
            }

            // Create consumer
            consumer = jmsSupport.createConsumer(session, dest, selector, connector.isNoLocal(), durableName, topic);
        } catch (JMSException e) {
            throw new ConnectException(e, this);
        }
    }
}
