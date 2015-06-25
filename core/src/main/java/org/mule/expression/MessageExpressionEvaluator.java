/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.transformer.DataType;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns properties on the MuleMessage itself. The supported expressions map directly to methods on the message.
 * For example:
 * <ul>
 * <li>id - returns the value of {@link MuleMessage#getUniqueId()}</li>
 * <li>correlationId - returns the value of {@link MuleMessage#getCorrelationId()}</li>
 * <li>correlationGroupSize - returns the value of {@link MuleMessage#getCorrelationGroupSize()}</li>
 * <li>correlationSequence - returns the value of {@link MuleMessage#getCorrelationSequence()}</li>
 * <li>replyTo - returns the value of {@link MuleMessage#getReplyTo()}</li>
 * <li>payload - returns the value of {@link MuleMessage#getPayload()}</li>
 * <li>encoding - returns the value of {@link MuleMessage#getEncoding()}</li>
 * <li>exception - returns the value of <code>MuleMessage.getExceptionPayload().getException()</code> or null if there is no exception payload</li>
 * </ul>
 * If no expression is set the MuleMessage itself will be returned.
 * <p/>
 * If the object passed in is not a MuleMessage, the same object will be returned.
 *
 * @see ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "message";
    public static final String PAYLOAD = "payload";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(MessagePayloadExpressionEvaluator.class);

    public Object evaluate(String expression, MuleMessage message)
    {
        if (StringUtils.isEmpty(expression) || message==null)
        {
            return message;
        }
        else
        {
            if (expression.equals("id"))
            {
                return message.getUniqueId();
            }
            else if (expression.equals("correlationId"))
            {
                return message.getCorrelationId();
            }
            else if (expression.equals("correlationSequence"))
            {
                return message.getCorrelationSequence();
            }
            else if (expression.equals("correlationGroupSize"))
            {
                return message.getCorrelationGroupSize();
            }
            else if (expression.equals("replyTo"))
            {
                return message.getReplyTo();
            }
            else if (expression.equals(PAYLOAD))
            {
                return message.getPayload();
            }
            else if (expression.equals("encoding"))
            {
                return message.getEncoding();
            }
            else if (expression.equals("exception"))
            {
                ExceptionPayload ep = message.getExceptionPayload();
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

    @Override
    public TypedValue evaluateTyped(String expression, MuleMessage message)
    {
        Object value = evaluate(expression, message);
        DataType dataType = expression.equals(PAYLOAD)? message.getDataType() : DataTypeFactory.create(value == null ? Object.class : value.getClass(), null);

        return new TypedValue(value, dataType);
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

}
