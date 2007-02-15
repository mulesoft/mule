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

import java.util.regex.Pattern;

/**
 * <code>RegExFilter</code> is used to match a String argument against a regular
 * expression.
 */

public class RegExFilter implements UMOFilter, ObjectFilter
{
    private Pattern expression;

    public RegExFilter()
    {
        super();
    }

    public RegExFilter(String pattern)
    {
        this.expression = Pattern.compile(pattern);
    }

    public boolean accept(UMOMessage message)
    {
        return accept(message.getPayload());
    }

    public boolean accept(Object object)
    {
        if (object == null)
        {
            return false;
        }

        return (expression != null && expression.matcher(object.toString()).find());
    }

    public String getExpression()
    {
        return (expression == null ? null : expression.pattern());
    }

    public void setExpression(String expression)
    {
        this.expression = (expression != null ? Pattern.compile(expression) : null);
    }

}
