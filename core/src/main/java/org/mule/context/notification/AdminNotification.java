/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>AdminNotification</code> is used to invoke actions on a remote mule server
 */

public class AdminNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -53091546441476249L;
    public static final int ACTION_RECEIVE = ADMIN_EVENT_ACTION_START_RANGE + 1;
    public static final int ACTION_DISPATCH = ADMIN_EVENT_ACTION_START_RANGE + 2;
    public static final int ACTION_SEND = ADMIN_EVENT_ACTION_START_RANGE + 3;
    public static final int ACTION_INVOKE = ADMIN_EVENT_ACTION_START_RANGE + 4;

    static {
        registerAction("receive event", ACTION_RECEIVE);
        registerAction("dispatch event", ACTION_DISPATCH);
        registerAction("send event", ACTION_SEND);
        registerAction("invoke service", ACTION_INVOKE);
    }

    private Map properties = new HashMap();
    private MuleMessage message;

    public AdminNotification(MuleMessage message, int action)
    {
        super(message, action);
    }

    public AdminNotification(MuleMessage message, int action, String resourceIdentifier)
    {
        super(message, action, resourceIdentifier);
        this.message = message;
    }

    public MuleMessage getMessage()
    {
        return message;
    }

    public void setProperty(Object key, Object value)
    {
        properties.put(key, value);
    }

    public Object getProperty(Object key)
    {
        return properties.get(key);
    }

    public Map getProperties()
    {
        return properties;
    }
}
