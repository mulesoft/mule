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

import org.mule.impl.MuleDescriptor;
import org.mule.umo.manager.UMOServerEvent;

/**
 * <code>ComponentEvent</code> is fired when an event such as the component starting
 * occurs.  The payload of this event will always be a reference to the component Descriptor.
 *
 * @see org.mule.impl.MuleDescriptor
 * @see org.mule.umo.UMODescriptor
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentEvent extends UMOServerEvent
{
    public static final int COMPONENT_INITIALISED = COMPONENT_EVENT_ACTION_START_RANGE + 1;
    public static final int COMPONENT_STARTED = COMPONENT_EVENT_ACTION_START_RANGE + 2;
    public static final int COMPONENT_STOPPED = COMPONENT_EVENT_ACTION_START_RANGE + 3;
    public static final int COMPONENT_PAUSED = COMPONENT_EVENT_ACTION_START_RANGE + 4;
    public static final int COMPONENT_RESUMED = COMPONENT_EVENT_ACTION_START_RANGE + 5;
    public static final int COMPONENT_DISPOSED = COMPONENT_EVENT_ACTION_START_RANGE + 6;

    private String[] actions = new String[]{
            "initialised","started","stopped",
            "paused","resumed","disposed"};

    public ComponentEvent(Object message, int action)
    {
        super(message, action);
    }

     protected String getPayloadToString()
    {
        return ((MuleDescriptor)source).getName();
    }

    protected String getActionName(int action)
    {
        int i = action - COMPONENT_EVENT_ACTION_START_RANGE;
        if(i-1 > actions.length) return String.valueOf(action);
        return actions[i-1];
    }
}
