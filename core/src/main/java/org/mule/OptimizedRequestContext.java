/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;

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
 *
 * @deprecated
 *    If access to MuleEvent or MuleMessage is required,
 *    then implement a {@link org.mule.api.processor.MessageProcessor}
 *    or {@link org.mule.api.lifecycle.Callable} instead
 */
@Deprecated
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
    public static MuleEvent unsafeSetEvent(MuleEvent event)
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
    public static MuleEvent criticalSetEvent(MuleEvent event)
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
    public static MuleMessage unsafeRewriteEvent(MuleMessage message)
    {
        documentUnsafeCall("unsafeRewriteEvent");
        return RequestContext.internalRewriteEvent(message, RequestContext.UNSAFE);
    }

    private static void documentUnsafeCall(String message)
    {
        if (DOCUMENT_UNSAFE_CALLS)
        {
            logger.debug(message, new Exception(message));
        }
    }
    
}
