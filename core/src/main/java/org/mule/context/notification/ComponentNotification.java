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

import org.mule.api.component.Component;
import org.mule.api.context.notification.ServerNotification;

/**
 * <code>ComponentNotification</code> is fired when an event such as the component
 * starting occurs. The payload of this event will always be a reference to the
 * component Descriptor.
 * 
 * @see org.mule.MuleDescriptor
 * @see org.mule.api.UMODescriptor
 */
public class ComponentNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8575741816897936674L;
    public static final int COMPONENT_INITIALISED = COMPONENT_EVENT_ACTION_START_RANGE + 1;
    public static final int COMPONENT_STARTED = COMPONENT_EVENT_ACTION_START_RANGE + 2;
    public static final int COMPONENT_STOPPED = COMPONENT_EVENT_ACTION_START_RANGE + 3;
    public static final int COMPONENT_PAUSED = COMPONENT_EVENT_ACTION_START_RANGE + 4;
    public static final int COMPONENT_RESUMED = COMPONENT_EVENT_ACTION_START_RANGE + 5;
    public static final int COMPONENT_DISPOSED = COMPONENT_EVENT_ACTION_START_RANGE + 6;
    public static final int COMPONENT_STOPPING = COMPONENT_EVENT_ACTION_START_RANGE + 7;

    static {
        registerAction("initialised", COMPONENT_INITIALISED);
        registerAction("started", COMPONENT_STARTED);
        registerAction("stopping", COMPONENT_STOPPING);
        registerAction("stopped", COMPONENT_STOPPED);
        registerAction("paused", COMPONENT_PAUSED);
        registerAction("resumed", COMPONENT_RESUMED);
        registerAction("disposed", COMPONENT_DISPOSED);
    }

    public ComponentNotification(Component message, int action)
    {
        super(message, action);
        resourceIdentifier = message.getName();
    }

    protected String getPayloadToString()
    {
        return ((Component) source).getName();
    }
}
