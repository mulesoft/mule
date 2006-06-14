/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.internal.notifications;

import org.mule.umo.manager.UMOManager;
import org.mule.umo.manager.UMOServerNotification;

/**
 * <code>ManagerNotification</code> is fired when an event such as the manager
 * starting occurs. The payload of this event will always be a reference to the
 * manager.
 * 
 * @see org.mule.MuleManager
 * @see org.mule.umo.manager.UMOManager
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ManagerNotification extends UMOServerNotification implements BlockingServerEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3246036188011581121L;
    public static final int MANAGER_INITIALISNG = MANAGER_EVENT_ACTION_START_RANGE + 1;
    public static final int MANAGER_INITIALISED = MANAGER_EVENT_ACTION_START_RANGE + 2;
    public static final int MANAGER_STARTING = MANAGER_EVENT_ACTION_START_RANGE + 3;
    public static final int MANAGER_STARTED = MANAGER_EVENT_ACTION_START_RANGE + 4;
    public static final int MANAGER_STOPPING = MANAGER_EVENT_ACTION_START_RANGE + 5;
    public static final int MANAGER_STOPPED = MANAGER_EVENT_ACTION_START_RANGE + 6;
    public static final int MANAGER_DISPOSING = MANAGER_EVENT_ACTION_START_RANGE + 7;
    public static final int MANAGER_DISPOSED = MANAGER_EVENT_ACTION_START_RANGE + 8;
    public static final int MANAGER_DISPOSING_CONNECTORS = MANAGER_EVENT_ACTION_START_RANGE + 9;
    public static final int MANAGER_DISPOSED_CONNECTORS = MANAGER_EVENT_ACTION_START_RANGE + 10;

    private static final transient String[] ACTIONS = new String[] { "initialising", "initialised", "starting",
            "started", "stopping", "stopped", "disposing", "disposed", "disposing connectors", "disposed connectors" };

    public ManagerNotification(UMOManager message, int action)
    {
        super(message, action);
        resourceIdentifier = message.getId();
    }

    protected String getPayloadToString()
    {
        return ((UMOManager) source).getId();
    }

    protected String getActionName(int action)
    {
        int i = action - MANAGER_EVENT_ACTION_START_RANGE;
        if (i - 1 > ACTIONS.length) {
            return String.valueOf(action);
        }
        return ACTIONS[i - 1];
    }

    public String toString() {
        return EVENT_NAME + "{" + "action=" + getActionName(action)
                + ", resourceId=" + resourceIdentifier + ", timestamp=" + timestamp + "}";
    }
}
