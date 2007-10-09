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

import org.mule.config.MuleProperties;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    // setting this to false gives old semantics in non-critical cases
    private static boolean SAFE = true;
    private static final Log logger = LogFactory.getLog(RequestContext.class);
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
        return internalSetEvent(newEvent(event, SAFE, false));
    }

    protected static UMOEvent internalSetEvent(UMOEvent event)
    {
        currentEvent.set(event);
        return event;
    }

    /**
     * Sets a new message payload in the RequestContext but maintains all other
     * properties (session, endpoint, synchronous, etc.) from the previous event.
     * Safe: use by default
     *
     * @param message - the new message payload
     * @return A new copy of the message set
     */
    public static UMOMessage rewriteEvent(UMOMessage message)
    {
        return internalRewriteEvent(message, SAFE, false);
    }

    protected static UMOMessage internalRewriteEvent(UMOMessage message, boolean safe, boolean required)
    {
        if (message != null)
        {
            UMOEvent event = getEvent();
            if (event != null)
            {
                UMOMessage copy = newMessage(message, safe, required);
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

    public static UMOMessage writeResponse(UMOMessage message)
    {
        return internalWriteResponse(message, SAFE, false);
    }

    protected static UMOMessage internalWriteResponse(UMOMessage message, boolean safe, boolean required)
    {
        if (message != null)
        {
            UMOEvent event = getEvent();
            if (event != null)
            {
                UMOMessage copy = newMessage(message, safe, required);
                combineProperties(event, copy);
                MuleEvent newEvent = new MuleEvent(copy, event.getEndpoint(), event.getSession(), event.isSynchronous());
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

    protected static void combineProperties(UMOEvent event, UMOMessage message)
    {
        for (Iterator iterator = event.getMessage().getPropertyNames().iterator(); iterator.hasNext();)
        {
            String key = (String) iterator.next();
            if (key == null)
            {
                logger.warn("Message property key is null: please report the following stack trace to dev@mule.codehaus.org.",
                        new IllegalArgumentException());
            }
            else
            {
                if (key.startsWith(MuleProperties.PROPERTY_PREFIX))
                {
                    Object newValue = message.getProperty(key);
                    Object oldValue = event.getMessage().getProperty(key);
                    if (newValue == null)
                    {
                        message.setProperty(key, oldValue);
                    }
                    else if (logger.isInfoEnabled() && !newValue.equals(oldValue))
                    {
                        logger.info("Message already contains property " + key + "=" + newValue
                                + " not replacing old value: " + oldValue);
                    }
                }
            }
        }
    }

    /**
     * Resets the current request context (clears all information).
     */
    public static void clear()
    {
        setEvent(null);
    }

    public static void setExceptionPayload(UMOExceptionPayload exceptionPayload)
    {
        getEvent().getMessage().setExceptionPayload(exceptionPayload);
    }

    public static UMOExceptionPayload getExceptionPayload()
    {
        return getEvent().getMessage().getExceptionPayload();
    }


    // utility methods for thread safe access

    protected static void noteUse(String type)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("copying " + type);//, new Exception());
        }
    }

    protected static UMOEvent newEvent(UMOEvent event, boolean safe, boolean required)
    {
        if (safe && event instanceof ThreadSafeAccess)
        {
            if (! required)
            {
                noteUse("event");
            }
            return (UMOEvent) ((ThreadSafeAccess)event).newThreadCopy();
        }
        else
        {
            return event;
        }
    }

    protected static UMOMessage newMessage(UMOMessage message, boolean safe, boolean required)
    {
        if (safe && message instanceof ThreadSafeAccess)
        {
            if (! required)
            {
                noteUse("message");
            }
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
