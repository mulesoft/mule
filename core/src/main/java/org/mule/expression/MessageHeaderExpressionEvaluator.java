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

import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.RequiredValueException;
import org.mule.config.i18n.CoreMessages;

/**
 * Looks up the property on the message using the property name given.  If the call on the messgae returns null,
 * parameters on the inbound endpoint will also be checked.
 *
 * @see MessageHeadersListExpressionEvaluator
 * @see MessageHeadersExpressionEvaluator
 * @see ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageHeaderExpressionEvaluator implements ExpressionEvaluator, ExpressionConstants
{
    public static final String NAME = "header";

    public Object evaluate(String expression, MuleMessage message)
    {
        Object result;
        boolean required;
        if (expression.endsWith(OPTIONAL_ARGUMENT))
        {
            expression = expression.substring(0, expression.length() - OPTIONAL_ARGUMENT.length());
            required = false;
        }
        else
        {
            required = true;
        }
        result = message.getProperty(expression);
        //Should this fallback be in its own expression evaluator i.e. #[endpoint-param:foo] ??
        //I'm not sure becaus ethis way there is a fallback where the message doesn't have a value the
        //endpoint can define a default
        if (result == null && RequestContext.getEventContext() != null)
        {
            result = RequestContext.getEventContext().getEndpointURI().getParams().get(expression);
        }

        if (result == null && required)
        {
            throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull(NAME, expression));
        }
        return result;
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
        throw new UnsupportedOperationException();
    }
}