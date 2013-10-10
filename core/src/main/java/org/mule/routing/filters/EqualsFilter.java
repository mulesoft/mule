/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.filters;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.ObjectFilter;

/**
 * <code>EqualsFilter</code> is a filter for comparing two objects using the
 * equals() method.
 */
public class EqualsFilter implements Filter, ObjectFilter
{
    private Object pattern;

    public EqualsFilter()
    {
        super();
    }

    public EqualsFilter(Object compareTo)
    {
        this.pattern = compareTo;
    }

    public boolean accept(MuleMessage message)
    {
        return accept(message.getPayload());
    }

    public boolean accept(Object object)
    {
        if (object == null && pattern == null)
        {
            return true;
        }

        if (object == null || pattern == null)
        {
            return false;
        }

        return pattern.equals(object);
    }

    public Object getPattern()
    {
        return pattern;
    }

    public void setPattern(Object pattern)
    {
        this.pattern = pattern;
    }

}
