/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.util.StringUtils;

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

    protected void doConnect() throws Exception
    {
        // template method
    }

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
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        Session session = null;
        MessageConsumer consumer = null;

        try
        {
            final boolean topic = connector.getTopicResolver().isTopic(endpoint);

            JmsSupport support = connector.getJmsSupport();
            session = connector.getSession(false, topic);
            Destination dest = support.createDestination(session, endpoint);

            // Extract jms selector
            String selector = null;
            if (endpoint.getFilter() != null && endpoint.getFilter() instanceof JmsSelectorFilter)
            {
                final String expressionTemplate = ((JmsSelectorFilter) endpoint.getFilter()).getExpression();
                if (StringUtils.isNotBlank(expressionTemplate))
                {
                    selector = connector.getMuleContext().getExpressionManager().parse(expressionTemplate, null);
                }
            }
            else if (endpoint.getProperties() != null)
            {
                // still allow the selector to be set as a property on the endpoint
                // to be backward compatable
                final String expressionTemplate = (String) endpoint.getProperty(JmsConstants.JMS_SELECTOR_PROPERTY);
                if (StringUtils.isNotBlank(expressionTemplate))
                {
                    selector = connector.getMuleContext().getExpressionManager().parse(expressionTemplate, null);
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
                topic);

            try
            {
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

                return new DefaultMuleMessage(connector.getMessageAdapter(message), connector.getMuleContext());
            }
            catch (Exception e)
            {
                connector.handleException(e);
                return null;
            }
        }
        finally
        {
            connector.closeQuietly(consumer);
            connector.closeQuietly(session);
        }
    }

    protected void doDispose()
    {
        // template method
    }

}
