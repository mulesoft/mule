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

import org.mule.InitialisationException;
import org.mule.MuleException;
import org.mule.impl.MuleMessage;
import org.mule.providers.XaPollingMessageReceiver;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionProxy;
import org.mule.transaction.XaTransaction;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.XASession;
import javax.transaction.xa.XAResource;

/**
 * <p><code>XaJmsMessageReceiver</code> is a polling Jms message listener that
 * reads messages within an Xa Transaction
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class XaJmsMessageReceiver extends XaPollingMessageReceiver
{
    private MessageConsumer consumer;
    private JmsConnector connector;
    private XASession session;

    public XaJmsMessageReceiver(UMOConnector connector,
                                UMOComponent component,
                                UMOEndpoint endpoint,
                                MessageConsumer consumer,
                                XASession session)
            throws InitialisationException
    {
        this(connector, component, endpoint, consumer, session, new Long(100));
    }
    /**
     * @param connector
     * @param component
     * @param endpoint
     * @throws InitialisationException
     */
    public XaJmsMessageReceiver(UMOConnector connector,
                                UMOComponent component,
                                UMOEndpoint endpoint,
                                MessageConsumer consumer,
                                XASession session,
                                Long frequency)
            throws InitialisationException
    {
        super(connector, component, endpoint, frequency);
        this.session = session;

        if (endpoint.getConnector() instanceof JmsConnector)
        {
            this.connector = (JmsConnector) endpoint.getConnector();
        }
        else
        {
            throw new InitialisationException("The endpoint configured for this XA Message Receiver is not a JmsConnector");
        }
        if (endpoint.getEndpointURI() == null)
        {
            throw new InitialisationException("The endpoint endpointUri cannot be null when listening");
        }
        this.consumer = consumer;
        //when using transacted sessions, dispatcher Jms producers should
        //not be cached, instead they should be recreated for each request
        //setting this ensures that a new dispatcher is created for each
        //request
        this.connector.setDisposeDispatcherOnCompletion(true);
    }


    /* (non-Javadoc)
     * @see org.mule.providers.XaPollingMessageReceiver#getMessage()
     */
    protected Object getMessage()
    {
        try
        {
            return consumer.receive(getFrequency());
        }
        catch (JMSException e)
        {
            handleException("Failed to receive JMS Message: " + e.getMessage(), e);
            return null;
        }
    }


    /* (non-Javadoc)
     * @see org.mule.providers.XaPollingMessageReceiver#processMessage(java.lang.Object)
     */
    protected synchronized void processMessage(Object msg, XaTransaction transaction) throws UMOException
    {
        Message message = (Message) msg;

        if (logger.isDebugEnabled())
        {
            logger.debug("Message received it is of type: " + message.getClass().getName());
        }
        try
        {
            String correlationId = message.getJMSCorrelationID();
            //Xa calls need to be synchronous as the transaction is boud to the current
            //thread
            boolean synchronous = true;

            logger.debug("Message CorrelationId is: " + correlationId);
            logger.info("Jms Message Id is: " + message.getJMSMessageID());

            if (message.getJMSRedelivered())
            {
                logger.info("Message with correlationId: " + correlationId + " is redelivered. handing off to Exception Handler");
                handleMessageRedeliviered(message);
                return;
            }
            //begin a new transaction
            TransactionProxy trans = connector.beginTransaction(endpoint);
            TransactionCoordination.getInstance().bindTransaction(trans);

//            if (connector instanceof TransactionEnabledConnector)
//            {
//                if(session.getTransacted()) {
//                    trans = ((TransactionEnabledConnector) connector).beginTransaction(provider, session);
//                } else if(connector instanceof JmsConnector && ((JmsConnector)connector).getAcknowledgementMode()== Session.CLIENT_ACKNOWLEDGE) {
//                    trans = ((TransactionEnabledConnector) connector).beginTransaction(provider, message);
//                }
//            }
            UMOMessageAdapter adapter = connector.getMessageAdapter(message);

            routeMessage(new MuleMessage(adapter), trans, synchronous);

        }
        catch (Exception e1)
        {
            handleException(message,
                    new MuleException("Failed to create incoming message to an event: " + e1.getMessage(), e1));
        }

    }

    /**
     * Stops consuming messages
     */
    protected void stopConsumer() throws JMSException
    {
        consumer.close();
    }

    /**
     * @return Returns the consumer.
     */
    public MessageConsumer getConsumer()
    {
        return consumer;
    }

    /**
     * @param consumer The consumer to set.
     */
    public void setConsumer(MessageConsumer consumer)
    {
        this.consumer = consumer;
    }

    /* (non-Javadoc)
    * @see org.mule.providers.XaPollingMessageReceiver#getXAResource()
    */
    protected XAResource getXAResource() throws Exception
    {
        return session.getXAResource();
    }

    public void handleMessageRedeliviered(Message message)
    {
        String corrId = null;
        try
        {
            corrId = message.getJMSCorrelationID();
        }
        catch (JMSException e)
        {
            corrId = "could not read id: " + e.getMessage();
        }

        String msg = "Message was redelivered, most likely due to a transaction rollback. " +
                "Correlation id was: " + corrId;
        logger.warn(msg);
        MessageRedeliveredException exception = new MessageRedeliveredException(msg, session);
        handleException(message, exception);
    }
}
