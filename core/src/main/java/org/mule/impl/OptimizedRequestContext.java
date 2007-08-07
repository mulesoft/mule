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

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

/**
 * NOT FOR POUBLIC USE - please use the interface provided by RequestContext.
 * This is a temporary interface that helps provide an (optimized) fix for message
 * scribbling.
 *
 * <p>Mutating methods have three versions: default (RequestContext; makes and returns a new copy);
 * unsafe (doesn't make a copy, use only where certain no threading); critical (as safe, but
 * documents that threading a known issue).</p>
 */
public final class OptimizedRequestContext
{

    /**
     * Do not instanciate.
     */
    private OptimizedRequestContext()
    {
        super();
    }

    /**
     * Set an event for out-of-scope thread access.  Unsafe: use only when known to be single threaded.
     *
     * @param event - the event to set
     * @return The event set
     */
    public static UMOEvent unsafeSetEvent(UMOEvent event)
    {
        return RequestContext.internalSetEvent(event);
    }

    /**
     * Set an event for out-of-scope thread access.  Critical: thread safety known to be required
     *
     * @param event - the event to set
     * @return A new mutable copy of the event set
     */
    public static UMOEvent criticalSetEvent(UMOEvent event)
    {
        return RequestContext.internalSetEvent(RequestContext.newEvent(event, true, true));
    }

    /**
     * Sets a new message payload in the RequestContext but maintains all other
     * properties (session, endpoint, synchronous, etc.) from the previous event.
     * Unsafe: use only when known to be single threaded
     *
     * @param message - the new message payload
     * @return The message set
     */
    public static UMOMessage unsafeRewriteEvent(UMOMessage message)
    {
        return RequestContext.internalRewriteEvent(message, false, false);
    }

    /**
     * Sets a new message payload in the RequestContext but maintains all other
     * properties (session, endpoint, synchronous, etc.) from the previous event.
     * Critical: thread safety known to be required
     *
     * @param message - the new message payload
     * @return A new copy of the message set
     */
    public static UMOMessage criticalRewriteEvent(UMOMessage message)
    {
        return RequestContext.internalRewriteEvent(message, true, true);
    }

    public static UMOMessage unsafeWriteResponse(UMOMessage message)
    {
        return RequestContext.internalWriteResponse(message, false, false);
    }

    public static UMOMessage criticalWriteResponse(UMOMessage message)
    {
        return RequestContext.internalWriteResponse(message, true, true);
    }

}
