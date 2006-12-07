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

import java.util.regex.Pattern;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>RegExFilter</code> is used to match a String argument against a regular
 * expression.
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

        return (pattern != null ? pattern.matcher(object.toString()).find() : false);
    }

    public String getPattern()
    {
        return (pattern == null ? null : pattern.pattern());
    }

    public void setPattern(String pattern)
    {
        this.pattern = (pattern != null ? Pattern.compile(pattern) : null);
    }

}
