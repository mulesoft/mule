/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.LRUMap;

/**
 * Evaluates a regular expression matches against a message's payload and
 * returns the result:
 * <p/>
 * null: if the payload does not match
 * payload: if the payload matches and it does not contain an explicit capture
 * group.
 * Captured value: if the payload matches and it contain only one
 * explicit capture group
 * List of captured values: if the expression matches and it contains multiple
 * capture groups
 */
public class RegexExpressionEvaluator implements ExpressionEvaluator
{

    private static final String NAME = "regex";

    private static final int SINGLE_CAPTURE_GROUP = 1;
    private static final int NO_CAPTURE_GROUP = 0;

    // Caches compiled patterns to improve performance
    private Map<String, Pattern> patterns = Collections.synchronizedMap(new LRUMap(256));

    @Override
    public Object evaluate(String expression, MuleMessage message)
    {
        Matcher matcher = createMatcher(expression, message);

        Object result = null;

        if (matcher.matches())
        {
            if (matcher.groupCount() == NO_CAPTURE_GROUP || matcher.groupCount() == SINGLE_CAPTURE_GROUP)
            {
                result = matcher.group(matcher.groupCount());
            }
            else
            {
                String[] matchedValues = new String[matcher.groupCount()];

                for (int i = 1; i <= matcher.groupCount(); i++)
                {
                    matchedValues[i - 1] = matcher.group(i);
                }

                result = matchedValues;
            }
        }

        return result;
    }

    private Matcher createMatcher(String expression, MuleMessage message)
    {
        Pattern pattern = patterns.get(expression);

        if (pattern == null)
        {
            pattern = Pattern.compile(expression);
            patterns.put(expression, pattern);
        }

        String payload;
        try
        {
            payload = message.getPayloadAsString();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }

        return pattern.matcher(payload);
    }

    @Override
    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return NAME;
    }
}
