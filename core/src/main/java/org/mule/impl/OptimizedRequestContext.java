/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * NOT FOR PUBLIC USE - please use the interface provided by RequestContext.
 * This is a temporary interface that helps provide an (optimized) fix for message
 * scribbling.
 *
 * <p>Mutating methods have three versions: default (RequestContext; safe, makes and returns a new
 * copy - although this can be changed via {@link RequestContext#DEFAULT_ACTION});
 * unsafe (doesn't make a copy, use only where certain no threading); critical (safe,
 * documents that threading a known issue).</p>
 */
public final class OptimizedRequestContext
{

    private static final boolean DOCUMENT_UNSAFE_CALLS = false;
    private static final Log logger = LogFactory.getLog(OptimizedRequestContext.class);

    /**
     * Do not instantiate.
     */
    private OptimizedRequestContext()
    {
        // unused
    }

    /**
     * Set an event for out-of-scope thread access.  Unsafe: use only when known to be single threaded.
     *
     * @param event - the event to set
     * @return The event set
     */
    public static UMOEvent unsafeSetEvent(UMOEvent event)
    {
        documentUnsafeCall("unsafeSetEvent");
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
        return RequestContext.internalSetEvent(RequestContext.newEvent(event, RequestContext.SAFE));
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
        documentUnsafeCall("unsafeRewriteEvent");
        return RequestContext.internalRewriteEvent(message, RequestContext.UNSAFE);
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
        return RequestContext.internalRewriteEvent(message, RequestContext.SAFE);
    }

    public static UMOMessage unsafeWriteResponse(UMOMessage message)
    {
        documentUnsafeCall("unsafeWriteResponse");
        return RequestContext.internalWriteResponse(message, RequestContext.UNSAFE);
    }

    public static UMOMessage criticalWriteResponse(UMOMessage message)
    {
        return RequestContext.internalWriteResponse(message, RequestContext.SAFE);
    }

    private static void documentUnsafeCall(String message)
    {
        if (DOCUMENT_UNSAFE_CALLS)
        {
            logger.debug(message, new Exception(message));
        }
    }

}
