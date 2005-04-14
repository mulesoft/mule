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
package org.mule.impl.internal.events;

import org.mule.umo.UMOMessage;
import org.mule.umo.manager.UMOServerEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>AdminEvent</code> is used to invoke actions on a remote mule server
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class AdminEvent extends UMOServerEvent
{
    public static final int ACTION_RECEIVE = ADMIN_EVENT_ACTION_START_RANGE + 1;
    public static final int ACTION_DISPATCH = ADMIN_EVENT_ACTION_START_RANGE + 2;
    public static final int ACTION_SEND = ADMIN_EVENT_ACTION_START_RANGE + 3;
    public static final int ACTION_INVOKE = ADMIN_EVENT_ACTION_START_RANGE + 4;

    private String[] actions = new String[]{
            "receive event","dispatch event", "send event","invoke component"};

    private String endpoint;
    private Map properties = new HashMap();
    private UMOMessage message;
    public AdminEvent(int action, UMOMessage message, String endpoint)
    {
        super(message, action);
        this.message = message;
        this.endpoint = endpoint;
    }

    public UMOMessage getMessage() {
        return message;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public Map getProperties()
    {
        return properties;
    }

   protected String getActionName(int action)
    {
        int i = action - ADMIN_EVENT_ACTION_START_RANGE;
        if(i-1 > actions.length) return String.valueOf(action);
        return actions[i];
    }
}
