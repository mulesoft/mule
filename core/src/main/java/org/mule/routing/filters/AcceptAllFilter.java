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
 * A filter that accepts everything.
 */
public class AcceptAllFilter implements Filter, ObjectFilter
{
    public static final AcceptAllFilter INSTANCE = new AcceptAllFilter();

    public boolean accept(MuleMessage message)
    {
        return true;
    }

    public boolean accept(Object object)
    {
        return true;
    }
}
