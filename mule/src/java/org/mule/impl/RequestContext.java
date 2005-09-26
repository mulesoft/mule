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
package org.mule.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;

/**
 * <code>RequestContext</code> is a thread context where components can get
 * the current event or set response properties that will be sent on the
 * outgoing message.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class RequestContext
{
    private static ThreadLocal events = new ThreadLocal();

    private static ThreadLocal props = new ThreadLocal();

    public static UMOEventContext getEventContext()
    {
        UMOEvent event = getEvent();
        if (event != null) {
            return new MuleEventContext(event);
        } else {
            return null;
        }
    }

    public static UMOEvent getEvent()
    {
        return (UMOEvent) events.get();
    }

    public static synchronized void setEvent(UMOEvent event)
    {
        events.set(event);
        if(event!=null) props.set(event.getProperties());
    }

    public static void setProperty(String key, Object value)
    {
        Map properties = (Map) props.get();
        if (properties == null) {
            properties = new HashMap();
            props.set(properties);
        }
        properties.put(key, value);
    }

    public static Map getProperties()
    {
        return (Map) props.get();
    }

    public static Object getProperty(String key)
    {
        Map properties = (Map) props.get();
        if (properties == null) {
            return null;
        }
        return properties.get(key);
    }

    public static void rewriteEvent(UMOMessage message)
    {
        UMOEvent event = getEvent();
        if (event != null) {
            event = new MuleEvent(message, event);
            setEvent(event);
        }
    }

    public static Map clearProperties()
    {
        Map p = (Map) props.get();
        props.set(null);
        return p;
    }

    public static void clear()
    {
        setEvent(null);
        clearProperties();
    }

    public static void setExceptionPayload(UMOExceptionPayload exceptionPayload)
    {
        getEvent().getMessage().setExceptionPayload(exceptionPayload);
    }

    public static UMOExceptionPayload getExceptionPayload()
    {
        return getEvent().getMessage().getExceptionPayload();
    }

    public static Map getAttachments() {
        Map attachments = new HashMap();
        if(getEvent()!=null) {
            UMOMessage message = getEvent().getMessage();
            for (Iterator iterator = message.getAttachmentNames().iterator(); iterator.hasNext();) {
                String name = (String) iterator.next();
                attachments.put(name, message.getAttachment(name));
            }
        }
        return attachments;
    }
}
