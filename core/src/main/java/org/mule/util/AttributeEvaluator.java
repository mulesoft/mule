/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.collections.*;
import org.apache.commons.collections.CollectionUtils;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.routing.filters.WildcardFilter;

/**
 * This class acts as a wrapper for configuration attributes that support
 * simple text, expression or regular expressions. It can be extended to support other cases too.
 */
public class AttributeEvaluator
{
    private static final Collection regexSpecialCharacters = Arrays.asList("\\", "$", "^", ".", "*", "?", "[", "]");
    private WildcardFilter regexWildcardFilter;
    private boolean enableRegexSupport = false;
    private enum AttributeType { PLAIN_TEXT, REGEX, EXPRESSION }

    private final String attributeValue;
    private ExpressionManager expressionManager;
    private Pattern regexPattern;
    private AttributeType attributeType;

    public AttributeEvaluator(String attributeValue)
    {
        this.attributeValue = attributeValue;
    }
    
    public void initialize(final ExpressionManager expressionManager)
    {
        this.expressionManager = expressionManager;
        resolveAttributeType();
    }

    private void resolveAttributeType()
    {
        if (expressionManager.isExpression(this.attributeValue))
        {
            this.attributeType = AttributeType.EXPRESSION;
        }
        else if (enableRegexSupport && hasRegexCharactersAndCompiles())
        {
            this.attributeType = AttributeType.REGEX;
            this.regexWildcardFilter = new WildcardFilter(this.attributeValue);
        }
        else
        {
            this.attributeType = AttributeType.PLAIN_TEXT;
        }
    }

    private boolean hasRegexCharactersAndCompiles()
    {
        boolean hasRegexCharacters = CollectionUtils.find(regexSpecialCharacters,new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                return attributeValue.contains((CharSequence) object);
            }
        }) != null;
        boolean compiles = false;
        boolean hasWildcards = false;
        if (hasRegexCharacters)
        {
            try
            {
                this.regexPattern = Pattern.compile(attributeValue);
                compiles = true;
            }
            catch (Exception e)
            {
            }
            hasWildcards = attributeValue.startsWith("*") || attributeValue.endsWith("*");
        }
        return (hasRegexCharacters && compiles) || (hasRegexCharacters && hasWildcards);
    }

    public boolean isRegularExpression()
    {
        return attributeType.equals(AttributeType.REGEX);
    }

    public boolean isExpression()
    {
        return attributeType.equals(AttributeType.EXPRESSION);
    }

    public boolean isPlainText()
    {
        return attributeType.equals(AttributeType.PLAIN_TEXT);
    }
    
    public boolean matches(String value)
    {
        validateStateIsRegularExpression();
        return (this.regexPattern != null ? this.regexPattern.matcher(value).matches() : false) || this.regexWildcardFilter.accept(value);
    }

    public Pattern getRegexPattern()
    {
        validateStateIsRegularExpression();
        return regexPattern;
    }

    private void validateStateIsRegularExpression()
    {
        if (!isRegularExpression())
        {
            throw new IllegalStateException("attribute is not a regular expression");
        }
    }

    public Object resolveValue(MuleMessage message)
    {
        if (isRegularExpression())
        {
            throw new IllegalStateException("attribute is a regular expression");
        }
        if (isExpression())
        {
            return expressionManager.evaluate(attributeValue,message);
        }
        else
        {
            return getRawValue();
        }
    }

    public String getRawValue()
    {
        return attributeValue;
    }

    public AttributeEvaluator enableRegexSupport()
    {
        enableRegexSupport = true;
        return this;
    }
}
