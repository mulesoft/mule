/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.remoting.notification;

import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>RemoteDispatcherNotification</code> is used to invoke actions on a remote mule server
 */
@Deprecated
public class RemoteDispatcherNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -53091546441476249L;

    /** This is a low range since it was allocated before being refactred into the client module from core */
    public static final int REMOTE_DISPATCHER_EVENT_ACTION_START_RANGE = 600;

    public static final int ACTION_RECEIVE = REMOTE_DISPATCHER_EVENT_ACTION_START_RANGE + 1;
    public static final int ACTION_DISPATCH = REMOTE_DISPATCHER_EVENT_ACTION_START_RANGE + 2;
    public static final int ACTION_SEND = REMOTE_DISPATCHER_EVENT_ACTION_START_RANGE + 3;
    public static final int ACTION_INVOKE = REMOTE_DISPATCHER_EVENT_ACTION_START_RANGE + 4;
    public static final int ACTION_WIRE_FORMAT = REMOTE_DISPATCHER_EVENT_ACTION_START_RANGE + 5;

    static {
        registerAction("receive event", ACTION_RECEIVE);
        registerAction("dispatch event", ACTION_DISPATCH);
        registerAction("send event", ACTION_SEND);
        registerAction("invoke component", ACTION_INVOKE);
        registerAction("request wire format", ACTION_WIRE_FORMAT);
    }

    private Map properties = new HashMap();
    private MuleMessage message;

    public RemoteDispatcherNotification(MuleMessage message, int action)
    {
        super(cloneMessage(message), action);
    }

    public RemoteDispatcherNotification(MuleMessage message, int action, String resourceIdentifier)
    {
        super(cloneMessage(message), action, resourceIdentifier);
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
