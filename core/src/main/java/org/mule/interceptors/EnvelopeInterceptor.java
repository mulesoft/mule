/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptors;

import org.mule.umo.Invocation;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.UMOMessage;

/**
 * <code>EnvelopeInterceptor</code> is an intercepter that will fire before and after an
 * event is received.
 */
public abstract class EnvelopeInterceptor implements UMOInterceptor
{
    /**
     * This method is invoked before the event is processed
     * 
     * @param invocation the message invocation being processed
     */
    public abstract void before(Invocation invocation) throws UMOException;

    /**
     * This method is invoked after the event has been processed
     * 
     * @param invocation the message invocation being processed
     */
    public abstract void after(Invocation invocation) throws UMOException;

    public final UMOMessage intercept(Invocation invocation) throws UMOException
    {
        before(invocation);
        UMOMessage message = invocation.execute();
        invocation.setMessage(message);
        after(invocation);
        return message;
    }
}
