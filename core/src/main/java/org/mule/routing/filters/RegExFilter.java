/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

import java.util.regex.Pattern;

/**
 * <code>RegExFilter</code> is used to match a String argument against a regular
 * pattern.
 */

public class RegExFilter implements UMOFilter, ObjectFilter
{
    private Pattern pattern;

    public RegExFilter()
    {
        super();
    }

    public RegExFilter(String pattern)
    {
        this.pattern = Pattern.compile(pattern);
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

        return (pattern != null && pattern.matcher(object.toString()).find());
    }

    public String getPattern()
    {
        return (pattern == null ? null : pattern.pattern());
    }

    public void setPattern(String pattern)
    {
        this.pattern = (pattern != null ? Pattern.compile(pattern) : null);
    }

    /**
     * @return
     * @deprecated Use {@link #getPattern()} This method name was changed to be consistent with other filters
     */
    public String getExpression()
    {
        return getPattern();
    }

    /**
     * @param
     * @deprecated Use {@link #getPattern()} This method name was changed to be consistent with other filters
     */
    public void setExpression(String expression)
    {
        setPattern(expression);
    }

}
