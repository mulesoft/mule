/*
 * $Header$
 * $Revision$
 * $Date$
 * -----------------------------------------------------------------------------------------------------
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
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.TransactionEnabledConnector;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.transaction.TransactionProxy;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageAdapter;

import javax.jms.*;


/**
 * <code>JmsMessageReceiver</code> is a simple <code>javax.jms.MessageListener</code> implementation that receives a <code>javax.jms.Message</code> and creates a <code>MuleEvent</code> from it before dispatching the event.
 * <p/>
 * The <code>JmsMessageReceiver</code> should be suitable for all JMS Connector implementations.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class JmsMessageReceiver extends AbstractMessageReceiver implements MessageListener

{
    private MessageConsumer consumer;
    private Session session;
    int i = 0;

    public JmsMessageReceiver()
    {
    }


    public JmsMessageReceiver(JmsConnector connector,
                              UMOComponent component,
                              UMOEndpoint endpoint,
                              MessageConsumer consumer,
                              Session session) throws JMSException, InitialisationException
    {
        create(connector, component, endpoint);

        this.consumer = consumer;
        this.session = session;
        consumer.setMessageListener(this);
    }

    /* (non-Javadoc)
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    public void onMessage(Message message)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Message received it is of type: " + message.getClass().getName());
        }
        try
        {
            String correlationId = message.getJMSCorrelationID();
            //If the reply to is set we need to send a response
            //so make this a synchronous call
            boolean synchronous =  connector.isSynchronous();
            if(message.getJMSReplyTo()!=null) {
                synchronous = true;
            }

            logger.debug("Message CorrelationId is: " + correlationId);
            logger.info("Jms Message Id is: " + message.getJMSMessageID());

            if (message.getJMSRedelivered())
            {
                logger.info("Message with correlationId: " + correlationId + " is redelivered. handing off to Exception Handler");
                handleMessageRedeliviered(message);
                return;
            }
            //begin a new transaction if necessary
            TransactionProxy trans = null;
            if (connector instanceof TransactionEnabledConnector)
            {
                if(session.getTransacted()) {
                    trans = ((TransactionEnabledConnector) connector).beginTransaction(endpoint, session);
                } else if(connector instanceof JmsConnector && ((JmsConnector)connector).getAcknowledgementMode()== Session.CLIENT_ACKNOWLEDGE) {
                    trans = ((TransactionEnabledConnector) connector).beginTransaction(endpoint, message);
                }
            }
            UMOMessageAdapter adapter = connector.getMessageAdapter(message);
            
            routeMessage(new MuleMessage(adapter), trans, synchronous);

        }
        catch (Exception e1)
        {
            handleException(message,
                    new MuleException("Failed to create incoming message to an event: " + e1.getMessage(), e1));
        }
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

    public void doDispose() throws UMOException
    {
        try
        {
            if(consumer!=null) consumer.close();
        } catch (JMSException e)
        {
            logger.error("failed to close consumer: " + e.getMessage());
        }

        try
        {
            if(session!=null) session.close();
        } catch (JMSException e1)
        {
            logger.error("Failed to close session: " + e1.getMessage());
        }
    }

    protected boolean allowFilter(UMOFilter filter) throws UnsupportedOperationException
    {
        return (filter instanceof JmsSelectorFilter);
    }
}

