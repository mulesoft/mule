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

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.RequiredValueException;
import org.mule.config.i18n.CoreMessages;

import javax.activation.DataHandler;

/**
 * Looks up an attachment with the given name.
 *
 * @see MessageAttachmentsListExpressionEvaluator
 * @see MessageAttachmentsExpressionEvaluator
 * @see org.mule.api.expression.ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageAttachmentExpressionEvaluator implements ExpressionEvaluator, ExpressionConstants
{
    public static final String NAME = "attachment";

    public Object evaluate(String expression, MuleMessage message)
    {
        if (expression == null)
        {
            return null;
        }

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
        DataHandler dh = message.getAttachment(expression);

        if (dh == null && required)
        {
            throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull(NAME, expression));
        }
        return dh;
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