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
 */
public class RequestContext
{
    private static final Log logger = LogFactory.getLog(RequestContext.class);
    private static final ThreadLocal currentEvent = new ThreadLocal();

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
        return (UMOEvent)currentEvent.get();
    }

    public static void setEvent(UMOEvent event)
    {
        currentEvent.set(event);
    }

    /**
     * Sets a new message payload in the RequestContext but maintains all other
     * properties (session, endpoint, synchronous, etc.) from the previous event.
     * 
     * @param message - current message payload
     */
    public static void rewriteEvent(UMOMessage message)
    {
        if (message != null)
        {
            UMOEvent event = getEvent();
            if (event != null)
            {
                event = new MuleEvent(message, event);
                setEvent(event);
            }
        }
    }

    public static void writeResponse(UMOMessage message)
    {
        if (message != null)
        {
            UMOEvent event = getEvent();
            if (event != null)
            {
                for (Iterator iterator = event.getMessage().getPropertyNames().iterator(); iterator.hasNext();)
                {
                    String key = (String)iterator.next();
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

                event = new MuleEvent(message, event.getEndpoint(), event.getSession(), event.isSynchronous());
                setEvent(event);
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

}
