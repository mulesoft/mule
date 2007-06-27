/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>MessagePropertyFilter</code> can be used to filter against properties on
 * an event. This can be very useful as the event properties represent all the meta
 * information about the event from the underlying transport, so for an event
 * received over HTTP you can check for HTTP headers etc. The pattern should be
 * expressed as a key/value pair, i.e. "propertyName=value". If you want to compare
 * more than one property you can use the logic filters for And, Or and Not
 * expressions. By default the comparison is case sensitive; you can set the
 * <i>caseSensitive</i> property to override this.
 */
public class MessagePropertyFilter implements UMOFilter
{
    private boolean caseSensitive = true;
    private boolean not = false;

    private String propertyName;
    private String propertyValue;

    public MessagePropertyFilter()
    {
        super();
    }

    public MessagePropertyFilter(String expression)
    {
        setExpression(expression);
    }

    public boolean accept(UMOMessage message)
    {
        if (message == null)
        {
            return false;
        }

        Object value = message.getProperty(propertyName);

        if (value == null)
        {
            return compare(null, propertyValue);
        }
        else
        {
            return compare(value.toString(), propertyValue);
        }
    }

    protected boolean compare(String value1, String value2)
    {
        if (value1 == null && value2 != null && !"null".equals(value2) && not)
        {
            return true;
        }

        if (value1 == null)
        {
            value1 = "null";
        }

        if (value2 == null)
        {
            value2 = "null";
        }

        boolean result = false;

        if (caseSensitive)
        {
            result = value1.equals(value2);
        }
        else
        {
            result = value1.equalsIgnoreCase(value2);
        }

        return (not ? !result : result);
    }

    public String getExpression()
    {
        return propertyName + '=' + propertyValue;
    }

    public void setExpression(String expression)
    {
        int i = expression.indexOf('=');
        if (i == -1)
        {
            throw new IllegalArgumentException(
                "Pattern is malformed - it should be a key value pair, i.e. property=value: " + expression);
        }
        else
        {
            if (expression.charAt(i - 1) == '!')
            {
                not = true;
                propertyName = expression.substring(0, i - 1).trim();
            }
            else
            {
                propertyName = expression.substring(0, i).trim();
            }
            propertyValue = expression.substring(i + 1).trim();
        }
    }

    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }
    
    /**
     * All Filters that are configured via spring have to implement this method.
     */
    public void setPattern(String pattern)
    {
        this.setExpression(pattern);
    }
}
