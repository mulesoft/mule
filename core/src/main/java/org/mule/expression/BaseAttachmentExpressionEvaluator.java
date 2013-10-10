/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import static org.mule.expression.ExpressionConstants.OPTIONAL_ARGUMENT;

import javax.activation.DataHandler;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.RequiredValueException;
import org.mule.config.i18n.CoreMessages;

public abstract class BaseAttachmentExpressionEvaluator implements ExpressionEvaluator
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
