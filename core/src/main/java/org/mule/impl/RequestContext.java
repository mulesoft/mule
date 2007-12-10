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
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;

/**
 * <code>RequestContext</code> is a thread context where components can get the
 * current event or set response properties that will be sent on the outgoing
 * message.
 *
 * <p>RequestContext seems to be used to allow thread local mutation of events that
 * are not otherwise available in the scope.  so this is a good place to create a new
 * thread local copy - it will be read because supporting code is expecting mutation.</p>
 *
 */
public final class RequestContext
{
    // to clarify "safe" in constructors
    public static boolean SAFE = true;
    public static boolean UNSAFE = true;

    // setting this to false gives old (mutable) semantics in non-critical cases
    private static boolean DEFAULT_ACTION = SAFE;

    private static final ThreadLocal currentEvent = new ThreadLocal();

    /** Do not instanciate. */
    protected RequestContext()
    {
        // no-op
    }

    public static UMOEventContext getEventContext()
    {
        UMOEvent event = getEvent();
        if (event != null)
        {
            return new MuleEventContext(event);
        }
        else
        {
            return null;
        }
    }

    public static UMOEvent getEvent()
    {
        return (UMOEvent) currentEvent.get();
    }

    /**
     * Set an event for out-of-scope thread access.  Safe: use by default
     *
     * @param event - the event to set
     * @return A new mutable copy of the event set
     */
    public static UMOEvent setEvent(UMOEvent event)
    {
        return internalSetEvent(newEvent(event, DEFAULT_ACTION));
    }

    protected static UMOEvent internalSetEvent(UMOEvent event)
    {
        currentEvent.set(event);
        return event;
    }

    protected static UMOMessage internalRewriteEvent(UMOMessage message, boolean safe)
    {
        if (message != null)
        {
            UMOEvent event = getEvent();
            if (event != null)
            {
                UMOMessage copy = newMessage(message, safe);
                UMOEvent newEvent = new MuleEvent(copy, event);
                if (safe)
                {
                    resetAccessControl(copy);
                }
                internalSetEvent(newEvent);
                return copy;
            }
        }
        return message;
    }

    /**
     * Resets the current request context (clears all information).
     */
    public static void clear()
    {
        setEvent(null);
    }

    /**
     * There is no unsafe version of this because it shouldn't be performance critical
     *
     * @param exceptionPayload
     */
    public static void setExceptionPayload(UMOExceptionPayload exceptionPayload)
    {
        UMOEvent newEvent = newEvent(getEvent(), SAFE);
        newEvent.getMessage().setExceptionPayload(exceptionPayload);
        internalSetEvent(newEvent);
    }

    public static UMOExceptionPayload getExceptionPayload()
    {
        return getEvent().getMessage().getExceptionPayload();
    }

    public static UMOMessage safeMessageCopy(UMOMessage message)
    {
        return newMessage(message, SAFE);
    }

    protected static UMOEvent newEvent(UMOEvent event, boolean safe)
    {
        if (safe && event instanceof ThreadSafeAccess)
        {
            return (UMOEvent) ((ThreadSafeAccess)event).newThreadCopy();
        }
        else
        {
            return event;
        }
    }

    protected static UMOMessage newMessage(UMOMessage message, boolean safe)
    {
        if (safe && message instanceof ThreadSafeAccess)
        {
            return (UMOMessage) ((ThreadSafeAccess)message).newThreadCopy();
        }
        else
        {
            return message;
        }
    }

    protected static void resetAccessControl(Object object)
    {
        if (object instanceof ThreadSafeAccess)
        {
            ((ThreadSafeAccess) object).resetAccessControl();
        }
    }

}
