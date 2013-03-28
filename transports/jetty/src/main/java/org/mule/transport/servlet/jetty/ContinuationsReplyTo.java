/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mortbay.util.ajax.Continuation;

/**
 * This class wraps a continuation object and provides a way to synchronize the access to it by a mutex lock object.
 */
public class ContinuationsReplyTo
{
    private Continuation continuation;
    private Object mutex;

    public ContinuationsReplyTo(Continuation continuation, Object mutex)
    {
        this.continuation = continuation;
        this.mutex = mutex;
    }
    
    public void setAndResume(Object value)
    {
        synchronized(mutex)
        {
            continuation.setObject(value);
            continuation.resume();
        }
    }
}


