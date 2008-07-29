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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.context.notification.ServerNotification;

/**
 * These notifications are fired when before and after a service component is
 * invoked.
 */
public class ComponentMessageNotification extends ServerNotification
{

    private static final long serialVersionUID = -6369685122731797646L;

    public static final int COMPONENT_PRE_INVOKE = COMPONENT_EVENT_ACTION_START_RANGE + 1;
    public static final int COMPONENT_POST_INVOKE = COMPONENT_EVENT_ACTION_START_RANGE + 2;

    static
    {
        registerAction("component pre invoke", COMPONENT_PRE_INVOKE);
        registerAction("component post invoke", COMPONENT_POST_INVOKE);
    }

    protected transient Component component;

    /**
     * @param message
     * @param action
     */
    public ComponentMessageNotification(MuleMessage message, Component component, int action)
    {
        super(cloneMessage(message), action);
        this.component = component;
        resourceIdentifier = component.getService().getName();

    }

    protected static MuleMessage cloneMessage(MuleMessage message)
    {
        // TODO we probably need to support deep cloning here
        synchronized (message)
        {
            return new DefaultMuleMessage(message.getPayload(), message);
        }
    }

    protected String getPayloadToString()
    {
        try
        {
            return ((MuleMessage) source).getPayloadAsString();
        }
        catch (Exception e)
        {
            return source.toString();
        }
    }

    /**
     * @return the message
     */
    public Component getComponent()
    {
        return component;
    }

    public String toString()
    {
        return EVENT_NAME + "{action=" + getActionName(action) + ", message: " + source + ", resourceId="
               + resourceIdentifier + ", timestamp=" + timestamp + ", serverId=" + serverId + ", component: "
               + component + "}";
    }

}
