/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import javax.jms.*;
import javax.transaction.xa.XAResource;

/**
 * <code>JmsMessageDispatcher</code> is responsible for dispatching
 * messages to Jms destinations. All Jms sematics apply and settings such
 * as replyTo and QoS properties are read from the event properties or defaults
 * are used (according to the Jms specification) 
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class JmsMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(JmsMessageDispatcher.class);

    private JmsConnector connector;
    private Session session;
    private Session receiveSession;
    private MessageProducer producer;
    private MessageConsumer consumer;

    public JmsMessageDispatcher(JmsConnector connector)
    {
        super(connector);
        this.connector = connector;
        //eventTimeout = MuleManager.getInstance().getConfiguration().getDefaultEventTimeout();
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
//        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
//        if(tx != null && (tx instanceof JmsTransaction || tx instanceof XaTransaction))
//        {
//            session = (Session)tx.getResource();
//        } else if(session==null) {
//            session = connector.getSession(false);
//        }

        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        XaTransaction xaTransaction = null;
        if(tx instanceof XaTransaction)
        {
            xaTransaction = (XaTransaction)tx;
            session = connector.getSession(false);
            xaTransaction.enlistResource(((XASession)session).getXAResource());
        } else if(tx instanceof JmsTransaction )
        {
           session = (Session)tx.getResource();
        } else if (session==null) {
             session = connector.getSession(false);
        }

        boolean syncReceive = event.getBooleanProperty(MuleProperties.MULE_SYNCHRONOUS_RECEIVE_PROPERTY,
                        MuleManager.getConfiguration().isSynchronousReceive());

        MessageConsumer replyToConsumer = null;
        UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
        //determine if endpointUri is a queue or topic
        //the format is topc:destination
        boolean topic = false;
        String resourceInfo = endpointUri.getResourceInfo();
        topic = (resourceInfo!=null && "topic".equalsIgnoreCase(resourceInfo));

        Destination dest = connector.getJmsSupport().createDestination(session, endpointUri.getAddress(), topic);
        Object message = event.getTransformedMessage();
        if (message instanceof Message)
        {
            Message msg = (Message) message;
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
            String deliveryModeString = (String)event.removeProperty("DeliveryMode");

            if(ttlString==null && priorityString==null && deliveryModeString == null) {
                connector.getJmsSupport().send(producer, msg);
            } else {
                long ttl = Message.DEFAULT_TIME_TO_LIVE;
                int priority  = Message.DEFAULT_PRIORITY;
                int deliveryMode = Message.DEFAULT_DELIVERY_MODE;

                if(ttlString!=null) ttl = Long.parseLong(ttlString);
                if(priorityString!=null) priority = Integer.parseInt(priorityString);
                if(deliveryModeString!=null) deliveryMode = Integer.parseInt(deliveryModeString);

                connector.getJmsSupport().send(producer, msg, deliveryMode, priority, ttl);
            }
            if(xaTransaction!=null)
            {
                xaTransaction.delistResource(((XASession)session).getXAResource(), XAResource.TMSUCCESS);
            }
            connector.commitTransaction(event);

            if(replyToConsumer!=null && event.isSynchronous()) {
                try
                {
                    int timeout = event.getTimeout();
                    logger.debug("Waiting for return event for: " + timeout + " ms");
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
        else
        {
            throw new MuleException("Message is not a JMS message, it is of type: "
                    + message.getClass().getName()
                    + ". Check the transformer for this Connector: "  +connector.getName());
        }
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
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        Destination dest = null;
        boolean topic = false;
        String resourceInfo = endpointUri.getResourceInfo();
        topic = (resourceInfo!=null && "topic".equalsIgnoreCase(resourceInfo));

        if(tx != null )
        {
            receiveSession = (Session)tx.getResource();
            dest = connector.getJmsSupport().createDestination(receiveSession, endpointUri.getAddress(), topic);
            consumer = connector.getJmsSupport().createConsumer(receiveSession, dest);
        } else if(receiveSession==null) {
            receiveSession = connector.getSession(false);
            dest = connector.getJmsSupport().createDestination(receiveSession, endpointUri.getAddress(), topic);
            consumer = connector.getJmsSupport().createConsumer(receiveSession, dest);
        }

        dest = connector.getJmsSupport().createDestination(receiveSession, endpointUri.getAddress(), topic);
        Message message = null;

        try
        {
            if(consumer==null) {
                consumer = connector.getJmsSupport().createConsumer(receiveSession, dest);
            }
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
            e.printStackTrace();
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageDispatcher#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null && tx instanceof JmsTransaction)
        {
            return ((JmsTransaction) tx).getResource();
        }

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
            throw new MuleException("Failed to get Jms session: " + e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageDispatcher#getConnector()
     */
    public UMOConnector getConnector()
    {
        return connector;
    }

    public void doDispose() throws UMOException
    {
        try
        {
            if(producer!=null) producer.close();
        } catch (JMSException e)
        {
            logger.error("failed to close producer for Jms Message Dispatcher: " + e.getMessage());
        }
        producer=null;
        try
        {
            if(consumer!=null) consumer.close();
        } catch (JMSException e)
        {
            logger.error("Failed to close Jms Receiver for endpointUri: " + e.getMessage());
        }

        try
        {
            if(receiveSession!=null) receiveSession.close();
        } catch (JMSException e)
        {
            logger.error("failed to close producer for Jms Message Dispatcher: " + e.getMessage());
        }
        try
        {
            if(session!=null) session.close();
        } catch (JMSException e)
        {
            logger.error("failed to close producer for Jms Message Dispatcher: " + e.getMessage());
        }
        session=null;
    }
}
