/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transport.AbstractMessageRequester;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.transport.jms.filters.JmsSelectorFilter;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * <code>JmsMessageDispatcher</code> is responsible for dispatching messages to JMS
 * destinations. All JMS semantics apply and settings such as replyTo and QoS
 * properties are read from the event properties or defaults are used (according to
 * the JMS specification)
 */
public class JmsMessageRequester extends AbstractMessageRequester
{

    private JmsConnector connector;

    public JmsMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JmsConnector) endpoint.getConnector();
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    @Override
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        Session session = null;
        MessageConsumer consumer = null;
        boolean cleanupListenerRegistered = false;

        try
        {
            final boolean topic = connector.getTopicResolver().isTopic(endpoint);

            JmsSupport support = connector.getJmsSupport();
            final TransactionConfig transactionConfig = endpoint.getTransactionConfig();
            final Transaction tx = TransactionCoordination.getInstance().getTransaction();
            boolean transacted = transactionConfig != null && transactionConfig.isTransacted();

            session = connector.getSession(transacted, topic);

            if (transacted && !tx.isXA())
            {
                // register a session close listener
                final Session finalSession = session;
                getEndpoint().getMuleContext().registerListener(new TransactionNotificationListener<TransactionNotification>()
                {
                    @Override
                    public void onNotification(TransactionNotification txNotification)
                    {
                        final int txAction = txNotification.getAction();
                        final String txId = txNotification.getTransactionStringId();
                        if ((txAction == TransactionNotification.TRANSACTION_COMMITTED || txAction == TransactionNotification.TRANSACTION_ROLLEDBACK) &&
                            txId.equals(tx.getId())) {
                            connector.closeQuietly(finalSession);
                        }
                    }
                }, tx.getId());

                cleanupListenerRegistered = true;
            }

            Destination dest = support.createDestination(session, endpoint);

            // Extract jms selector
            String selector = null;
            JmsSelectorFilter selectorFilter = connector.getSelector(endpoint);
            if (selectorFilter != null)
            {
                final String expressionTemplate = selectorFilter.getExpression();
                if (StringUtils.isNotBlank(expressionTemplate))
                {
                    selector = getEndpoint().getMuleContext().getExpressionManager().parse(expressionTemplate, null);
                }
            }
            else if (endpoint.getProperties() != null)
            {
                // still allow the selector to be set as a property on the endpoint
                // to be backward compatable
                final String expressionTemplate = (String) endpoint.getProperty(JmsConstants.JMS_SELECTOR_PROPERTY);
                if (StringUtils.isNotBlank(expressionTemplate))
                {
                    selector = getEndpoint().getMuleContext().getExpressionManager().parse(expressionTemplate, null);
                }
            }
            String tempDurable = (String) endpoint.getProperties().get(JmsConstants.DURABLE_PROPERTY);
            boolean durable = connector.isDurable();
            if (tempDurable != null)
            {
                durable = Boolean.valueOf(tempDurable);
            }

            // Get the durable subscriber name if there is one
            String durableName = (String) endpoint.getProperties().get(JmsConstants.DURABLE_NAME_PROPERTY);
            if (durableName == null && durable && topic)
            {
                durableName = "mule." + connector.getName() + "." + endpoint.getEndpointURI().getAddress();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Jms Connector for this receiver is durable but no durable name has been specified. Defaulting to: "
                             + durableName);
                }
            }

            // Create consumer
            consumer = support.createConsumer(session, dest, selector, connector.isNoLocal(), durableName,
                topic, endpoint);

            Message message;

            if (timeout == JmsMessageDispatcher.RECEIVE_NO_WAIT)
            {
                message = consumer.receiveNoWait();
            }
            else if (timeout == JmsMessageDispatcher.RECEIVE_WAIT_INDEFINITELY)
            {
                message = consumer.receive();
            }
            else
            {
                message = consumer.receive(timeout);
            }

            if (message == null)
            {
                return null;
            }

            message = connector.preProcessMessage(message, session);
            return createMuleMessage(message, endpoint.getEncoding());
        }
        finally
        {
            if (!cleanupListenerRegistered)
            {
                connector.closeQuietly(consumer);
                connector.closeSessionIfNoTransactionActive(session);
            }
        }
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

}
