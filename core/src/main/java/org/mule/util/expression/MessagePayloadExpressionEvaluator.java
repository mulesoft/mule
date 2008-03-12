/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.expression;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageAdapter;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;

/**
 * Returns the message payload.  If the expression is set to a class name then Mule will attempt to transform the payload by
 * discovering the correct transformer(s) in the registry. This is only suited for simple transformations between common types.
 *
 * <code>
 * ${payload:byte[]}
 * </code>
 *
 * or
 *
 * <code>
 * ${payload:org.mule.api.OutputHandler}
 * </code>
 *
 * If the object passed in is not a MuleMessage, the same object will be returned.
 * 
 * @see ExpressionEvaluator
 * @see ExpressionEvaluatorManager
 */
public class MessagePayloadExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "payload";
    public static final String BYTE_ARRAY = "byte[]";

    public Object evaluate(String expression, Object message)
    {
        if (message instanceof MuleMessage)
        {
            if(expression==null)
            {
                return ((MuleMessage) message).getPayload();
            }
            else
            {
                try
                {
                    if(expression.equals(BYTE_ARRAY))
                    {
                        return ((MuleMessage) message).getPayload(byte[].class);
                    }
                    else
                    {
                        return ((MuleMessage) message).getPayload(ClassUtils.loadClass(expression, getClass()));
                    }
                }
                catch (TransformerException e)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToProcessExtractorFunction(expression), e);
                }
                catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        }
        if (message instanceof MessageAdapter)
        {
            return ((MessageAdapter) message).getPayload();
        }
        return message;
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }

    /** {@inheritDoc} */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }
}