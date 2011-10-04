/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts values from a given source using regular expressions and uses
 * that values to enrich the mule message.
 */
public class ValueExtractorTransformer extends AbstractMessageTransformer
{

    private String source;
    private List<ValueExtractorTemplate> valueExtractorTemplates;

    public ValueExtractorTransformer()
    {
        // Default constructor required by spring
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        String valueToMatch = getValueToMatch(message);

        for (ValueExtractorTemplate valueExtractorTemplate : valueExtractorTemplates)
        {
            Matcher matcher = valueExtractorTemplate.compiledPattern.matcher(valueToMatch);

            if (matcher.matches())
            {
                if (matcher.groupCount() < 1)
                {
                    throw new IllegalStateException("Matched regular expression does not contain the expected capture group");
                }

                //TODO: there should be a way to decide which group/groups values should be used to enrich the message
                muleContext.getExpressionManager().enrich(valueExtractorTemplate.getTarget(), message, matcher.group(1));
            }
            else if (valueExtractorTemplate.failIfNoMatch)
            {
                throw new IllegalStateException(String.format("Source value '%s' does not math pattern '%s'", valueToMatch, valueExtractorTemplate.getPattern()));
            }
        }

        return message;
    }

    private String getValueToMatch(MuleMessage message)
    {
        if (muleContext.getExpressionManager().isValidExpression(source))
        {
            Object result = muleContext.getExpressionManager().evaluate(source, message, true);

            if (result instanceof String)
            {
                return (String) result;
            }
            else
            {
                throw new IllegalArgumentException("String value expected but received value is " + result.getClass().getName());
            }
        }
        else
        {
            return source;
        }
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public List<ValueExtractorTemplate> getValueExtractorTemplates()
    {
        return Collections.unmodifiableList(valueExtractorTemplates);
    }

    public void setValueExtractorTemplates(List<ValueExtractorTemplate> ValueExtractorTemplates)
    {
        this.valueExtractorTemplates = ValueExtractorTemplates;
    }

    public static class ValueExtractorTemplate
    {

        private String pattern;
        private String target;
        private boolean failIfNoMatch;
        private Pattern compiledPattern;

        @SuppressWarnings({"UnusedDeclaration"})
        public ValueExtractorTemplate()
        {
            // Default constructor required by Spring
        }

        public ValueExtractorTemplate(String pattern, String target, boolean failIfNoMatch)
        {
            setPattern(pattern);
            this.target = target;
            this.failIfNoMatch = failIfNoMatch;
        }

        public String getPattern()
        {
            return pattern;
        }

        public void setPattern(String pattern)
        {
            this.pattern = pattern;
            compiledPattern = Pattern.compile(pattern);
        }

        public String getTarget()
        {
            return target;
        }

        public void setTarget(String target)
        {
            this.target = target;
        }

        public boolean isFailIfNoMatch()
        {
            return failIfNoMatch;
        }

        public void setFailIfNoMatch(boolean failIfNoMatch)
        {
            this.failIfNoMatch = failIfNoMatch;
        }
    }
}
