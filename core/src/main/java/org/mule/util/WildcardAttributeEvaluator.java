/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class WildcardAttributeEvaluator
{
    private String attributeValue;
    private String escapedValue;
    private Boolean hasWildcards;

    public WildcardAttributeEvaluator(String attributeValue)
    {
        if (attributeValue == null)
        {
            throw new IllegalArgumentException("null not allowed");
        }
        this.attributeValue = attributeValue;
        this.escapedValue = attributeValue.replaceAll("\\*","*");
        hasWildcards = attributeValue.startsWith("*") || (attributeValue.endsWith("*") && !attributeValue.endsWith("\\*"))|| attributeValue.equals("*");
    }

    public boolean hasWildcards()
    {
        return hasWildcards;
    }

    public void processValues(Collection<String> values, MatchCallback matchCallback)
    {
        if (!hasWildcards())
        {
            throw new IllegalStateException("Can't call processValues with non wildcard attribute");
        }
        String[] valuesArray = values.toArray(new String[values.size()]);
        for (String value : valuesArray)
        {
            if (matches(value))
            {
                matchCallback.processMatch(value);
            }
        }
    }

    private boolean matches(String value)
    {
        if (value == null)
        {
            return false;
        }
        if (escapedValue.equals("*"))
        {
            return true;
        } 
        else if (escapedValue.startsWith("*"))
        {
            return value.endsWith(escapedValue.substring(1, escapedValue.length()));
        }
        else if (escapedValue.endsWith("*"))
        {
            return value.startsWith(escapedValue.substring(0, escapedValue.length()-1));
        }
        return false;
    }

    public interface MatchCallback 
    {
        public void processMatch(String matchedValue);
    }
}
