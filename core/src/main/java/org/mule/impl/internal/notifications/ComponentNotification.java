/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.impl.MuleDescriptor;
import org.mule.umo.UMODescriptor;
import org.mule.umo.manager.UMOServerNotification;

/**
 * <code>ComponentNotification</code> is fired when an event such as the component
 * starting occurs. The payload of this event will always be a reference to the
 * component Descriptor.
 * 
 * @see org.mule.impl.MuleDescriptor
 * @see org.mule.umo.UMODescriptor
 */
public class ComponentNotification extends UMOServerNotification
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

    private static final transient String[] ACTIONS = new String[]{"initialised", "started", "stopped",
        "paused", "resumed", "disposed", "stopping"};

    public ComponentNotification(UMODescriptor message, int action)
    {
        super(message, action);
        resourceIdentifier = message.getName();
    }

    protected String getPayloadToString()
    {
        return ((MuleDescriptor)source).getName();
    }

    protected String getActionName(int action)
    {
        int i = action - COMPONENT_EVENT_ACTION_START_RANGE;
        if (i - 1 > ACTIONS.length)
        {
            return String.valueOf(action);
        }
        return ACTIONS[i - 1];
    }
}
