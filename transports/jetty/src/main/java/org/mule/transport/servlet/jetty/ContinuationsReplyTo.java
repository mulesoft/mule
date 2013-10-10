/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


