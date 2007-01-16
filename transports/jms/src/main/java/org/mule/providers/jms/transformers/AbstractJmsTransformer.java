/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.transformers;

import java.util.Iterator;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.mule.config.MuleProperties;
import org.mule.impl.RequestContext;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsConstants;
import org.mule.providers.jms.JmsMessageUtils;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>AbstractJmsTransformer</code> is an abstract class that should be used for
 * all transformers where a JMS message will be the transformed or transformee
 * object. It provides services for compressing and uncompressing messages.
 */

public abstract class AbstractJmsTransformer extends AbstractTransformer
{

    public AbstractJmsTransformer()
    {
        super();
    }

    protected Message transformToMessage(Object src) throws TransformerException
    {
        try
        {
            Message result;

            if (src instanceof Message)
            {
                result = (Message)src;
                result.clearProperties();
            }
            else
            {
                result = JmsMessageUtils.toMessage(src, this.getSession());
            }

            // set the event properties on the Message
            UMOEventContext ctx = RequestContext.getEventContext();
            if (ctx == null)
            {
                logger.warn("There is no current event context");
                return result;
            }

            this.setJmsProperties(ctx.getMessage(), result);

            return result;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected Object transformFromMessage(Message source) throws TransformerException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message type received is: " + source.getClass().getName());
            }

            // Try to figure out our endpoint's JMS Specification and fall back to
            // 1.0.2 if none is set.
            String jmsSpec = JmsConstants.JMS_SPECIFICATION_102B;
            UMOImmutableEndpoint endpoint = this.getEndpoint();
            if (endpoint != null)
            {
                UMOConnector connector = endpoint.getConnector();
                if (connector instanceof JmsConnector)
                {
                    jmsSpec = ((JmsConnector)connector).getSpecification();
                }
            }

            return JmsMessageUtils.toObject(source, jmsSpec);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void setJmsProperties(UMOMessage umoMessage, Message msg) throws JMSException
    {
        for (Iterator iterator = umoMessage.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String key = iterator.next().toString();

            if (!JmsConstants.JMS_PROPERTY_NAMES.contains(key))
            {
                Object value = umoMessage.getProperty(key);

                if (MuleProperties.MULE_CORRELATION_ID_PROPERTY.equals(key))
                {
                    msg.setJMSCorrelationID(umoMessage.getCorrelationId());
                }

                // We dont want to set the ReplyTo property again as it will be set
                // using JMSReplyTo
                if (!(MuleProperties.MULE_REPLY_TO_PROPERTY.equals(key) && value instanceof Destination))
                {
                    // sanitize key as JMS header
                    key = JmsMessageUtils.encodeHeader(key);

                    try
                    {
                        msg.setObjectProperty(key, value);
                    }
                    catch (JMSException e)
                    {
                        // Various JMS servers have slightly different rules to what
                        // can be set as an object property on the message; therefore
                        // we have to take a hit n' hope approach
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Unable to set property '" + key + "' of type "
                                            + value.getClass().getName() + "': " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    protected Session getSession() throws UMOException
    {
        if (endpoint != null)
        {
            // TODO AP: use the right JMS session creation arguments (if required)
            return (Session)endpoint.getConnector().getDelegateSession(endpoint, null);
        }
        else
        {
            throw new TransformerException(this, new IllegalStateException(
                "This transformer needs a valid endpoint"));
        }
    }

}
