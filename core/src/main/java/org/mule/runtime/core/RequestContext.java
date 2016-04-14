/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;

/**
 * <code>RequestContext</code> is a thread context where components can get the
 * current event or set response properties that will be sent on the outgoing
 * message.
 *
 * <p>RequestContext seems to be used to allow thread local mutation of events that
 * are not otherwise available in the scope.  so this is a good place to create a new
 * thread local copy - it will be read because supporting code is expecting mutation.</p>
 *
 * @deprecated
 *    If access to MuleEvent or MuleMessage is required,
 *    then implement a {@link org.mule.api.processor.MessageProcessor}
 *    or {@link org.mule.api.lifecycle.Callable} instead
 */
@Deprecated
public final class RequestContext
{
    // to clarify "safe" in constructors
    public static final boolean SAFE = true;
    public static final boolean UNSAFE = true;

    // setting this to false gives old (mutable) semantics in non-critical cases
    private static final boolean DEFAULT_ACTION = SAFE;

    private static final ThreadLocal currentEvent = new ThreadLocal();

    /** Do not instanciate. */
    protected RequestContext()
    {
        // no-op
    }

    public static MuleEventContext getEventContext()
    {
        MuleEvent event = getEvent();
        if (event != null && !VoidMuleEvent.getInstance().equals(event))
        {
            return new DefaultMuleEventContext(event);
        }
        else
        {
            return null;
        }
    }

    public static MuleEvent getEvent()
    {
        return (MuleEvent) currentEvent.get();
    }

    /**
     * Set an event for out-of-scope thread access.  Safe: use by default
     *
     * @param event - the event to set
     * @return A new mutable copy of the event set
     */
    public static MuleEvent setEvent(MuleEvent event)
    {
        return internalSetEvent(newEvent(event, DEFAULT_ACTION));
    }

    protected static MuleEvent internalSetEvent(MuleEvent event)
    {
        currentEvent.set(event);
        return event;
    }

    protected static MuleMessage internalRewriteEvent(MuleMessage message, boolean safe)
    {
        if (message != null)
        {
            MuleEvent event = getEvent();
            if (event != null)
            {
                MuleMessage copy = newMessage(message, safe);
                MuleEvent newEvent = new DefaultMuleEvent(copy, event);
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
    public static void setExceptionPayload(ExceptionPayload exceptionPayload)
    {
        MuleEvent newEvent = newEvent(getEvent(), SAFE);
        newEvent.getMessage().setExceptionPayload(exceptionPayload);
        internalSetEvent(newEvent);
    }

    public static ExceptionPayload getExceptionPayload()
    {
        return getEvent().getMessage().getExceptionPayload();
    }

    public static MuleMessage safeMessageCopy(MuleMessage message)
    {
        return newMessage(message, SAFE);
    }

    protected static MuleEvent newEvent(MuleEvent event, boolean safe)
    {
        if (safe && event instanceof ThreadSafeAccess)
        {
            return (MuleEvent) ((ThreadSafeAccess)event).newThreadCopy();
        }
        else
        {
            return event;
        }
    }

    protected static MuleMessage newMessage(MuleMessage message, boolean safe)
    {
        if (safe && message instanceof ThreadSafeAccess)
        {
            return (MuleMessage) ((ThreadSafeAccess)message).newThreadCopy();
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
