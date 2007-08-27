/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.impl.RequestContext;

/**
 * <code>Invocation</code> represents a link in an interceptor chain. Interceptors
 * can be configured om Mule Managed components.
 */
// @ThreadSafe
public class Invocation
{
    /** The components descriptor */
    // @GuardedBy(itself)
    private final UMOImmutableDescriptor descriptor;

    /** the next invocation in the chain */
    // @GuardedBy(itself)
    private final Invocation invocation;

    /** The current message for the component */
    // @GuardedBy(this)
    private UMOMessage message;

    /**
     * Constructs an initialised invocation
     * 
     * @param descriptor the components descriptor
     * @param message the current message
     * @param invocation the next invocation in the chain or null.
     */
    public Invocation(UMOImmutableDescriptor descriptor, UMOMessage message, Invocation invocation)
    {
        this.descriptor = descriptor;
        this.message = message;
        this.invocation = invocation;
    }

    /**
     * Excutes this invocation
     * 
     * @return the current message that may have been altered by the invocation
     * @throws UMOException if something goes wrong
     */
    public UMOMessage execute() throws UMOException
    {
        return invocation.execute();
    }

    /**
     * Returns the descriptor for the component associated with this invocation
     * 
     * @return the descriptor for the component associated with this invocation
     */
    public UMOImmutableDescriptor getDescriptor()
    {
        return descriptor;
    }

    public UMOEvent getEvent()
    {
        return RequestContext.getEvent();
    }

    /**
     * Returns the current message
     * 
     * @return the current message
     */
    public UMOMessage getMessage()
    {
        synchronized (this)
        {
            return message;
        }
    }

    public void setMessage(UMOMessage message)
    {
        synchronized (this)
        {
            this.message = message;
        }
    }

}
