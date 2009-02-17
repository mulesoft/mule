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

import org.mule.api.MessagingException;
import org.mule.transport.jms.i18n.JmsMessages;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This redelivery handler will keep counting the redelivery attempts for each message redelivered. Used for
 * providers not implementing the {@code JMSXDeliveryCount} property support.
 */
public class CountingRedeliveryHandler implements RedeliveryHandler
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(CountingRedeliveryHandler.class);

    private Map<String, Integer> messages = null;

    protected JmsConnector connector;

    @SuppressWarnings("unchecked")
    public CountingRedeliveryHandler()
    {
        messages = Collections.synchronizedMap(new LRUMap(256));
    }

    /**
     * The connector associated with this handler is set before
     * <code>handleRedelivery()</code> is called
     * 
     * @param connector the connector associated with this handler
     */
    public void setConnector(JmsConnector connector)
    {
        this.connector = connector;
    }

    /**
     * process the redelivered message. If the Jms receiver should process the
     * message, it should be returned. Otherwise the connector should throw a
     * <code>MessageRedeliveredException</code> to indicate that the message should
     * be handled by the connector Exception Handler.
     * 
     */
    public void handleRedelivery(Message message) throws JMSException, MessagingException
    {
        if (connector.getMaxRedelivery() <= 0)
        {
            return;
        }

        String id = message.getJMSMessageID();

        if (id == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message doesn't have a JMSMessageID set, Mule can't handle redelivery for it. " + message);
            }
            return;
        }

        Integer redeliveryCount = messages.remove(id);
        if (redeliveryCount != null)
        {
            redeliveryCount += 1; // inc the count
        }

        if (redeliveryCount == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message with id: " + id + " has been redelivered for the first time");
            }
            messages.put(id, 1);
        }
        else if (redeliveryCount > connector.getMaxRedelivery())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(MessageFormat.format(
                        "Message with id: {0} has been redelivered {1} times, which exceeds the maxRedelivery setting " +
                        "of {2} on the connector {3}", id, redeliveryCount, connector.getMaxRedelivery(), connector.getName()));
            }
            JmsMessageAdapter adapter = (JmsMessageAdapter) connector.getMessageAdapter(message);
            throw new MessageRedeliveredException(
                    JmsMessages.tooManyRedeliveries(id, "" + redeliveryCount, connector.getMaxRedelivery(),
                                                    connector.getName()), adapter);

        }
        else
        {
            messages.put(id, redeliveryCount);
            if (logger.isDebugEnabled())
            {
                logger.debug("Message with id: " + id + " has been redelivered " + redeliveryCount + " times");
            }
        }
    }
}
