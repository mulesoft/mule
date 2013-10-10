/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import static org.mule.expression.ExpressionConstants.ALL_ARGUMENT;
import static org.mule.expression.ExpressionConstants.DELIM;
import static org.mule.expression.ExpressionConstants.OPTIONAL_ARGUMENT;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.RequiredValueException;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.filters.WildcardFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.activation.DataHandler;

/**
 * Looks up the attachment(s) on the message using the expression given. The
 * expression can contain a comma-separated list of header names to lookup. A
 * <code>java.util.Map&lt;String, DataHandler&gt;</code> of key value pairs is
 * returned.
 *
 * @see MessageAttachmentsListExpressionEvaluator
 * @see MessageAttachmentExpressionEvaluator
 * @see ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageAttachmentsExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "attachments";

    public Object evaluate(String expression, MuleMessage message)
    {
        boolean required;

        Map<String, DataHandler> result;
        //Enable wildcard matching
        if (expression.contains(ALL_ARGUMENT))
        {
            WildcardFilter filter = new WildcardFilter(expression);
            result = new HashMap<String, DataHandler>(message.getInboundAttachmentNames().size());
            for (String name : message.getInboundAttachmentNames())
            {
                if (filter.accept(name))
                {
                    result.put(name, message.getInboundAttachment(name));
                }
            }
        }
        else
        {
            StringTokenizer tokenizer = new StringTokenizer(expression, DELIM);
            result = new HashMap<String, DataHandler>(tokenizer.countTokens());
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
                    result.put(s, val);
                }
                else if (required)
                {
                    throw new RequiredValueException(CoreMessages.expressionEvaluatorReturnedNull(NAME, expression));
                }
            }
        }
        if (result.size() == 0)
        {
            return Collections.unmodifiableMap(Collections.<String, DataHandler>emptyMap());
        }
        else
        {
            return Collections.unmodifiableMap(result);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

}
