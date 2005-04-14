/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl;

import org.mule.umo.*;

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
            UMOInterceptor interceptor = (UMOInterceptor) interceptors.get(cursor);
            incCursor();
            message = interceptor.intercept(this);
            return message;
        }
        return getMessage();
    }

    private synchronized void incCursor() {
        cursor++;
    }

}
