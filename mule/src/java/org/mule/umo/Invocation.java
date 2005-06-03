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
package org.mule.umo;

import org.mule.impl.RequestContext;

/**
 * <code>Invocation</code> represents a link in an interceptor chain.
 * Interceptors can be configured om Mule Managed components.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Invocation
{
    /** The components descriptor */
    private UMOImmutableDescriptor descriptor;

    /** The current message for the component */
    private UMOMessage message;

    /** he next invocation in the chain */
    private Invocation invocation;

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

    /**
     * Returns the current message
     * 
     * @return the current message
     */
    public UMOMessage getMessage()
    {
        return message;
    }

    public UMOEvent getEvent()
    {
        return RequestContext.getEvent();
    }

    public synchronized void setMessage(UMOMessage message)
    {
        this.message = message;
    }
}
