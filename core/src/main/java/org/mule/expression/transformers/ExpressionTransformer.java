/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.NullPayload;

import java.util.Iterator;

/**
 * This transformer will evaluate one or more expressions on the current message and return the
 * results as an Array. If only one expression is defined it will return the object returned from
 * the expression.
 * <p/>
 * You can use expressions to extract
 * <ul>
 * <li>headers (single, map or list)</li>
 * <li>attachments (single, map or list)</li>
 * <li>payload</li>
 * <li>xpath</li>
 * <li>groovy</li>
 * <li>bean</li>
 * </ul>
 * and more.
 * <p/>
 * This transformer provides a very powerful way to pull different bits of information from the
 * message and pass them to the service.
 */
public class ExpressionTransformer extends AbstractExpressionTransformer
{
    private boolean returnSourceIfNull = false;

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object results[] = new Object[arguments.size()];
        int i = 0;
        for (Iterator<ExpressionArgument> iterator = arguments.iterator(); iterator.hasNext(); i++)
        {
            ExpressionArgument argument = iterator.next();
            try
            {
                results[i] = argument.evaluate(message);
            }
            catch (RequiredValueException e)
            {
                if(!argument.isOptional())
                {
                    throw e;
                }
                logger.warn(e.getMessage());
            }
            catch (ExpressionRuntimeException e)
            {
                throw new TransformerException(this, e);
            }

            if (!argument.isOptional() && results[i] == null)
            {
                throw new TransformerException(CoreMessages.expressionEvaluatorReturnedNull(
                        argument.getExpressionConfig().getEvaluator(), argument.getExpressionConfig().getExpression()), this);

            }

        }
        if (isReturnSourceIfNull() && checkIfAllAreNull(results))
        {
            return message;
        }

        if (results.length == 1)
        {
            return results[0];
        }
        else
        {
            return results;
        }
    }

    private boolean checkIfAllAreNull(Object[] objects)
    {
        for (int i = 0; i < objects.length; i++)
        {
            if (objects[i] != null && !(objects[i] instanceof NullPayload))
            {
                return false;
            }
        }
        return true;
    }

    public boolean isReturnSourceIfNull()
    {
        return returnSourceIfNull;
    }

    public void setReturnSourceIfNull(boolean returnSourceIfNull)
    {
        this.returnSourceIfNull = returnSourceIfNull;
    }
}
