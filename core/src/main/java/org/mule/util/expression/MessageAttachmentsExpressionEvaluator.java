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

import org.mule.api.transport.MessageAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

/**
 * Looks up the attachment(s) on the message using the expression given. The expression can contain a comma-separated list
 * of header names to lookup. A {@link java.util.Map} of key value pairs is returned.
 *
 * @see MessageAttachmentsListExpressionEvaluator
 * @see MessageAttachmentExpressionEvaluator
 * @see ExpressionEvaluator
 * @see ExpressionEvaluatorManager
 */
public class MessageAttachmentsExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "attachments";
    public static final String DELIM = ",";

    public Object evaluate(String expression, Object message)
    {
        if (message instanceof MessageAdapter)
        {
            StringTokenizer tokenizer = new StringTokenizer(expression, DELIM);
            Map result = new HashMap(tokenizer.countTokens());
            while(tokenizer.hasMoreTokens())
            {
                String s = tokenizer.nextToken();
                s = s.trim();
                Object val = ((MessageAdapter) message).getAttachment(s);
                if (val != null)
                {
                    result.put(s, val);
                }
            }
            if (result.size() == 0)
            {
                return null;
            }
            else
            {
                return result;
            }
        }
        return null;
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