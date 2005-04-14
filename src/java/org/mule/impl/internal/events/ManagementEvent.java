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

import org.mule.umo.manager.UMOServerEvent;

/**
 * <code>ManagementEvent</code> is fired when monitored resources such as internal queues reach capacity
 *
 * @see org.mule.MuleManager
 * @see org.mule.umo.manager.UMOManager
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ManagementEvent extends UMOServerEvent
{
    //todo resource status events here i.e.
    public static final int MANAGEMENT_COMPONENT_QUEUE_EXHAUSTED = MANAGEMENT_EVENT_ACTION_START_RANGE + 1;
    public static final int MANAGEMENT_NODE_PING = MANAGEMENT_EVENT_ACTION_START_RANGE + 2;

    private String[] actions = new String[]{};

    public ManagementEvent(Object message, int action)
    {
        super(message, action);
    }

    protected String getActionName(int action)
    {
        int i = action - MANAGEMENT_EVENT_ACTION_START_RANGE;
        if(i-1 > actions.length) return String.valueOf(action);
        return actions[i-1];
    }
}
