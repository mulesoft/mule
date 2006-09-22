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

import org.mule.umo.manager.UMOServerNotification;

/**
 * <code>ManagementNotification</code> is fired when monitored resources such as
 * internal queues reach capacity
 * 
 * @see org.mule.MuleManager
 * @see org.mule.umo.manager.UMOManager
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ManagementNotification extends UMOServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -259130553709035786L;

    // TODO resource status notifications here i.e.
    public static final int MANAGEMENT_COMPONENT_QUEUE_EXHAUSTED = MANAGEMENT_EVENT_ACTION_START_RANGE + 1;
    public static final int MANAGEMENT_NODE_PING = MANAGEMENT_EVENT_ACTION_START_RANGE + 2;

    private static final transient String[] ACTIONS = new String[] {};

    public ManagementNotification(Object message, int action)
    {
        super(message, action);
    }

    protected String getActionName(int action)
    {
        int i = action - MANAGEMENT_EVENT_ACTION_START_RANGE;
        if (i - 1 > ACTIONS.length) {
            return String.valueOf(action);
        }
        return ACTIONS[i - 1];
    }
}
