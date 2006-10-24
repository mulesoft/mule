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
 * <code>InterceptorsInvoker</code> is used trigger an interceptor chain.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class InterceptorsInvoker extends Invocation
{
    private List interceptors;
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
        UMOMessage message = null;
        if (cursor < interceptors.size())
        {
            UMOInterceptor interceptor = (UMOInterceptor)interceptors.get(cursor);
            incCursor();
            message = interceptor.intercept(this);
            return message;
        }
        return getMessage();
    }

    private synchronized void incCursor()
    {
        cursor++;
    }

}
