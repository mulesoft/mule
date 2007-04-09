/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.umo.Invocation;
import org.mule.umo.UMOException;
import org.mule.umo.UMOImmutableDescriptor;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.UMOMessage;

import java.util.List;

/**
 * <code>InterceptorsInvoker</code> is used to trigger an interceptor chain.
 */

public class InterceptorsInvoker extends Invocation
{
    private final List interceptors;
    private int cursor = 0;

    public InterceptorsInvoker(List interceptors, MuleDescriptor descriptor, UMOMessage message)
    {
        this(interceptors, new ImmutableMuleDescriptor(descriptor), message);
    }

    public InterceptorsInvoker(List interceptors, UMOImmutableDescriptor descriptor, UMOMessage message)
    {
        super(descriptor, message, null);
        this.interceptors = interceptors;
    }

    public UMOMessage execute() throws UMOException
    {
        if (cursor < interceptors.size())
        {
            UMOInterceptor interceptor = (UMOInterceptor) interceptors.get(cursor);
            incCursor();
            return interceptor.intercept(this);
        }
        return getMessage();
    }

    private synchronized void incCursor()
    {
        cursor++;
    }

}
