/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.expression;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.transport.MessageAdapter;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns properties on the MuleMessage itself. The supported expressions map directly to methods on the message.
 * For example:
 * <ul>
 * <li>id - returns the value of {@link org.mule.api.MuleMessage#getUniqueId()}</li>
 * <li>correlationId - returns the value of {@link org.mule.api.MuleMessage#getCorrelationId()}</li>
 * <li>correlationGroupSize - returns the value of {@link org.mule.api.MuleMessage#getCorrelationGroupSize()}</li>
 * <li>correlationSequence - returns the value of {@link org.mule.api.MuleMessage#getCorrelationSequence()}</li>
 * <li>replyTo - returns the value of {@link org.mule.api.MuleMessage#getReplyTo()}</li>
 * <li>payload - returns the value of {@link org.mule.api.MuleMessage#getPayload()}</li>
 * <li>encoding - returns the value of {@link org.mule.api.MuleMessage#getEncoding()}</li>
 * <li>exception - returns the value of {@link org.mule.api.MuleMessage#getExceptionPayload().getException()} or null if there is no exception payload</li>
 * </ul>
 * If no expression is set the MuleMessage itself will be returned.
 * <p/>
 * If the object passed in is not a MuleMessage, the same object will be returned.
 *
 * @see org.mule.api.expression.ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "message";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(MessagePayloadExpressionEvaluator.class);

    public Object evaluate(String expression, MuleMessage message)
    {
        if (message instanceof MuleMessage)
        {
            if (StringUtils.isEmpty(expression))
            {
                return message;
            }
            else
            {
                if (expression.equals("id"))
                {
                    return ((MuleMessage) message).getUniqueId();
                }
                else if (expression.equals("correlationId"))
                {
                    return ((MuleMessage) message).getCorrelationId();
                }
                else if (expression.equals("correlationSequence"))
                {
                    return ((MuleMessage) message).getCorrelationSequence();
                }
                else if (expression.equals("correlationGroupSize"))
                {
                    return ((MuleMessage) message).getCorrelationGroupSize();
                }
                else if (expression.equals("replyTo"))
                {
                    return ((MuleMessage) message).getReplyTo();
                }
                else if (expression.equals("payload"))
                {
                    return ((MuleMessage) message).getPayload();
                }
                else if (expression.equals("encoding"))
                {
                    return ((MuleMessage) message).getEncoding();
                }
                else if (expression.equals("exception"))
                {
                    ExceptionPayload ep = ((MuleMessage) message).getExceptionPayload();
                    if (ep != null)
                    {
                        return ep.getException();
                    }
                    return null;
                }
                else
                {
                    throw new IllegalArgumentException(expression);
                }

            }
        }
        else
        {
            logger.warn("Message is not of type MuleMessage, the expression will return the object without modification");
            if (message instanceof MessageAdapter)
            {
                return ((MessageAdapter) message).getPayload();
            }
        }
        return message;

    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }
}