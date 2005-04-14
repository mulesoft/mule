/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
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

import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;

import javax.jms.*;

/**
 * <code>JmsMessageDispatcher</code> is responsible for dispatching
 * messages to Jms destinations. All Jms sematics apply and settings such
 * as replyTo and QoS properties are read from the event properties or defaults
 * are used (according to the Jms specification) 
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JmsMessageDispatcher extends AbstractMessageDispatcher
{

    private JmsConnector connector;
    private Session session;
    private Session receiveSession;
    private MessageProducer producer;
    private MessageConsumer consumer;

    public JmsMessageDispatcher(JmsConnector connector)
    {
        super(connector);
        this.connector = connector;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#dispatchEvent(org.mule.MuleEvent, org.mule.providers.MuleEndpoint)
     */
    public void doDispatch(UMOEvent event) throws Exception
    {
        dispatchMessage(event);
    }

    private UMOMessage dispatchMessage(UMOEvent event) throws Exception
    {
        if(logger.isDebugEnabled()) {
            logger.debug("dispatching on endpoint: " + event.getEndpoint().getEndpointURI() + ". Event id is: " + event.getId());
        }
            // If a jms session can be bound to the current transaction,
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        boolean transacted = false;
        if(tx==null) {
            transacted = event.getEndpoint().getTransactionConfig()!=null;
        } else {
            transacted = (tx instanceof XaTransaction);
        }
    	Session session = connector.getSession(tx!=null);

        boolean syncReceive = event.getBooleanProperty(MuleProperties.MULE_SYNCHRONOUS_RECEIVE_PROPERTY,
                        							   MuleManager.getConfiguration().isSynchronousReceive());
        if (tx != null && syncReceive) {
        	throw new IllegalTransactionStateException(new org.mule.config.i18n.Message("jms", 2));
        }

        MessageConsumer replyToConsumer = null;
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        //determine if endpointUri is a queue or topic
        //the format is topc:destination
        boolean topic = false;
        String resourceInfo = endpointUri.getResourceInfo();
        topic = (resourceInfo!=null && "topic".equalsIgnoreCase(resourceInfo));

        Destination dest = connector.getJmsSupport().createDestination(session, endpointUri.getAddress(), topic);
        Object message = event.getTransformedMessage();

         if (!(message instanceof Message))
        {
            throw new DispatchException(new org.mule.config.i18n.Message(Messages.MESSAGE_NOT_X_IT_IS_TYPE_X_CHECK_TRANSFORMER_ON_X,
                            "JMS message", message.getClass().getName(), connector.getName()), event.getMessage(), event.getEndpoint());
        }

        Message msg = (Message) message;
        if(event.getMessage().getCorrelationId()!=null) {
            msg.setJMSCorrelationID(event.getMessage().getCorrelationId());
        }
        Destination replyTo = null;

        Object tempReplyTo = event.removeProperty("JMSReplyTo");
        if(tempReplyTo!=null)
        {
            if(tempReplyTo instanceof Destination) {
                replyTo = (Destination)tempReplyTo;
            } else {
                boolean replyToTopic = false;
                String reply = tempReplyTo.toString();
                int i = reply.indexOf(":");
                if(i > -1) {
                    String qtype = reply.substring(0, i);
                    replyToTopic = "topic".equalsIgnoreCase(qtype);
                    reply = reply.substring(i+1);
                }
                replyTo = connector.getJmsSupport().createDestination(session, reply, replyToTopic);
            }
            msg.setJMSReplyTo(replyTo);
         }
            //Are we going to wait for a return event?
         if(syncReceive && replyTo==null) {
            replyTo = connector.getJmsSupport().createTemporaryDestination(session, topic);
            msg.setJMSReplyTo(replyTo);
        }
        if(replyTo!=null) {
            replyToConsumer = connector.getJmsSupport().createConsumer(session, replyTo);
        }

        if(producer==null) {
            producer = connector.getJmsSupport().createProducer(session, dest);
        }

        //QoS support
        String ttlString = (String)event.removeProperty("TimeToLive");
        String priorityString = (String)event.removeProperty("Priority");
        String persistentDeliveryString = (String)event.removeProperty("PersistentDelivery");

        if(ttlString==null && priorityString==null && persistentDeliveryString == null) {
            connector.getJmsSupport().send(producer, msg);
        } else {
            long ttl = Message.DEFAULT_TIME_TO_LIVE;
            int priority  = Message.DEFAULT_PRIORITY;
            boolean persistent = Message.DEFAULT_DELIVERY_MODE==DeliveryMode.PERSISTENT;

            if(ttlString!=null) ttl = Long.parseLong(ttlString);
            if(priorityString!=null) priority = Integer.parseInt(priorityString);
            if(persistentDeliveryString!=null) persistent = Boolean.valueOf(persistentDeliveryString).booleanValue();

            connector.getJmsSupport().send(producer, msg, persistent, priority, ttl);
        }

        if(replyToConsumer!=null && event.isSynchronous()) {
            try
            {
                int timeout = event.getTimeout();
                logger.debug("Waiting for return event for: " + timeout + " ms on " + replyTo);
                Message result = replyToConsumer.receive(timeout);
                if(result==null){
                    logger.debug("No message was returned via replyTo destination");
                    return null;
                } else {
                    Object resultObject = JmsMessageUtils.getObjectForMessage(result);
                    return new MuleMessage(resultObject, null);
                }
            } finally
            {
                replyToConsumer.close();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#sendEvent(org.mule.MuleEvent, org.mule.providers.MuleEndpoint)
     */
    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        UMOMessage message = dispatchMessage(event);
        return message;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#sendEvent(org.mule.MuleEvent, org.mule.providers.MuleEndpoint)
     */
    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        UMOTransaction tx =TransactionCoordination.getInstance().getTransaction();
    	Session receiveSession = connector.getSession(tx instanceof XaTransaction);
        
        Destination dest = null;
        boolean topic = false;
        String resourceInfo = endpointUri.getResourceInfo();
        topic = (resourceInfo!=null && "topic".equalsIgnoreCase(resourceInfo));

        dest = connector.getJmsSupport().createDestination(receiveSession, endpointUri.getAddress(), topic);
        consumer = connector.getJmsSupport().createConsumer(receiveSession, dest);

        Message message = null;
        try
        {
            if(timeout == RECEIVE_NO_WAIT) {
                message = consumer.receiveNoWait();
            } else if(timeout == RECEIVE_WAIT_INDEFINITELY) {
                message = consumer.receive();
            } else {
                message = consumer.receive(timeout);
            }
            if(message==null) return null;
            return new MuleMessage(connector.getMessageAdapter(message));
        } catch (Exception e) {
            connector.getExceptionListener().exceptionThrown(e);
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageDispatcher#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        try
        {
            if(session==null )
            {
                session = connector.getSession(false);
            }
            return session;
        }
        catch (Exception e)
        {
            throw new MuleException(new org.mule.config.i18n.Message("jms", 3), e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageDispatcher#getConnector()
     */
    public UMOConnector getConnector()
    {
        return connector;
    }

    public void doDispose()
    {
    	JmsUtils.closeQuietly(producer);
    	JmsUtils.closeQuietly(consumer);
    	JmsUtils.closeQuietly(receiveSession);
    	JmsUtils.closeQuietly(session);
        producer = null;
        consumer = null;
        receiveSession = null;
        session = null;
    }
}
