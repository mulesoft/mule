/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.transaction.TransactionCoordination;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.transport.ConnectException;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.JmsConstants;
import org.mule.transport.jms.JmsMessageUtils;
import org.mule.util.ClassUtils;

import java.util.Iterator;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * <code>AbstractJmsTransformer</code> is an abstract class that should be used for
 * all transformers where a JMS message will be the transformed or transformee
 * object. It provides services for compressing and uncompressing messages.
 */

public abstract class AbstractJmsTransformer extends AbstractMessageAwareTransformer implements DiscoverableTransformer
{

    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public AbstractJmsTransformer()
    {
        super();
        declareInputOutputClasses();
    }

    protected abstract void declareInputOutputClasses();
    
    protected Message transformToMessage(MuleMessage message) throws TransformerException
    {
        Session session = null;
        try
        {
            Message result;

            Object src = message.getPayload();
            if (src instanceof Message)
            {
                result = (Message) src;
                result.clearProperties();
            }
            else
            {
                session = this.getSession();
                result = JmsMessageUtils.toMessage(src, session);
            }
            this.setJmsProperties(message, result);

            return result;
        }
        catch (TransformerException tex)
        {
            // rethrow
            throw tex;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
        finally
        {
            /*
                session.getTransacted() would be easier in most cases, but e.g. in Weblogic 8.x
                Java EE apps there could be some quirks, see http://forums.bea.com/thread.jspa?threadID=200007643
                to get a picture.

                Though JmsTransaction has this session.getTransacted() validation already, we're taking extra precautions
                to cover XA cases and potentially to make up for a configuration error. E.g. omitting transaction
                configuration from an outbound endpoint or router. Note, XA support in Mule will deliberately
                fail with fanfares to signal this case, which is really a user error.
              */

            if (session != null && endpoint != null) // endpoint can be null in some programmatic tests only in fact
            {
                Transaction muleTx = TransactionCoordination.getInstance().getTransaction();

                final JmsConnector connector = (JmsConnector) endpoint.getConnector();
                if (muleTx == null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Closing non-transacted jms session: " + session);
                    }
                    connector.closeQuietly(session);
                }
                else if (!muleTx.hasResource(connector.getConnection()))
                {
                    // this is some other session from another connection, don't let it leak
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Closing an orphaned, but transacted jms session: " + session +
                                ", transaction: " + muleTx);
                    }
                    connector.closeQuietly(session);
                }
            }
            // aggressively killing any session refs
            session = null;
        }
    }

    protected Object transformFromMessage(Message source, String encoding) throws TransformerException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message type received is: " +
                        ClassUtils.getSimpleName(source.getClass()));
            }

            // Try to figure out our endpoint's JMS Specification and fall back to
            // 1.0.2 if none is set.
            String jmsSpec = JmsConstants.JMS_SPECIFICATION_102B;
            ImmutableEndpoint endpoint = this.getEndpoint();
            if (endpoint != null)
            {
                Connector connector = endpoint.getConnector();
                if (connector instanceof JmsConnector)
                {
                    jmsSpec = ((JmsConnector) connector).getSpecification();
                }
            }

            return JmsMessageUtils.toObject(source, jmsSpec, encoding);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void setJmsProperties(MuleMessage message, Message msg) throws JMSException
    {
        for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String key = iterator.next().toString();

            if (!JmsConstants.JMS_PROPERTY_NAMES.contains(key))
            {
                Object value = message.getProperty(key);

                if (MuleProperties.MULE_CORRELATION_ID_PROPERTY.equals(key))
                {
                    msg.setJMSCorrelationID(message.getCorrelationId());
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
                                    + ClassUtils.getSimpleName(value.getClass())
                                    + "': " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    protected Session getSession() throws TransformerException, ConnectException, JMSException
    {
        if (endpoint != null)
        {
            return ((JmsConnector) endpoint.getConnector()).getSession(endpoint);
        }
        else
        {
            throw new TransformerException(this, new IllegalStateException(
                    "This transformer needs a valid endpoint"));
        }
    }

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
}
