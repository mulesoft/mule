/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.RequiredValueException;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.filters.WildcardFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.activation.DataHandler;

import static org.mule.expression.ExpressionConstants.ALL_ARGUMENT;
import static org.mule.expression.ExpressionConstants.DELIM;
import static org.mule.expression.ExpressionConstants.OPTIONAL_ARGUMENT;

/**
 * Looks up the attachment(s) on the message using the expression given. The expression can contain a comma-separated list
 * of header names to lookup. A {@link java.util.List} of values is returned.
 *
 * @see MessageAttachmentsExpressionEvaluator
 * @see MessageAttachmentExpressionEvaluator
 * @see org.mule.api.expression.ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageAttachmentsListExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "attachments-list";

    public Object evaluate(String expression, MuleMessage message)
    {
        boolean required;

        List<DataHandler> result;
        //Enable Wildcard matching
        if (expression.contains(ALL_ARGUMENT))
        {
            WildcardFilter filter = new WildcardFilter(expression);
            result = new ArrayList<DataHandler>(message.getInboundAttachmentNames().size());
            for (String name : message.getInboundAttachmentNames())
            {
                if (filter.accept(name))
                {
                    result.add(message.getInboundAttachment(name));
                }
            }
        }
        else
        {
            StringTokenizer tokenizer = new StringTokenizer(expression, DELIM);
            result = new ArrayList<DataHandler>(tokenizer.countTokens());
            while (tokenizer.hasMoreTokens())
            {
                String s = tokenizer.nextToken();
                s = s.trim();
                if (s.endsWith(OPTIONAL_ARGUMENT))
                {
                    s = s.substring(0, s.length() - OPTIONAL_ARGUMENT.length());
                    required = false;
                }
                else
                {
                    required = true;
                }
                DataHandler val = message.getInboundAttachment(s);
                if (val != null)
                {
                    result.add(val);
                }
                else if (required)
                {
                    throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull(NAME, expression));
                }
            }
        }
        if (result.size() == 0)
        {
            return Collections.unmodifiableList(Collections.<DataHandler>emptyList());
        }
        else
        {
            return Collections.unmodifiableList(result);
        }
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
