/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.mule.expression.ExpressionConstants.OPTIONAL_ARGUMENT;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.config.i18n.CoreMessages;

import javax.activation.DataHandler;

public abstract class BaseAttachmentExpressionEvaluator extends AbstractExpressionEvaluator
{
    
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
        DataHandler dh = getAttachment(message, expression);

        if (dh == null && required)
        {
            throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull(getName(), expression));
        }
        return dh;
    }

    protected abstract DataHandler getAttachment(MuleMessage message, String attachmentName);

}
