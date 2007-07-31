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

import org.mule.umo.UMOManagementContext;
import org.mule.umo.manager.UMOServerNotification;

/**
 * <code>ManagerNotification</code> is fired when an event such as the manager
 * starting occurs. The payload of this event will always be a reference to the
 *managementContext.
 *
 */
public class ManagerNotification extends UMOServerNotification implements BlockingServerEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3246036188011581121L;
    public static final int MANAGER_INITIALISING = MANAGER_EVENT_ACTION_START_RANGE + 1;
    public static final int MANAGER_INITIALISED = MANAGER_EVENT_ACTION_START_RANGE + 2;
    public static final int MANAGER_STARTING = MANAGER_EVENT_ACTION_START_RANGE + 3;
    public static final int MANAGER_STARTED = MANAGER_EVENT_ACTION_START_RANGE + 4;
    public static final int MANAGER_STOPPING = MANAGER_EVENT_ACTION_START_RANGE + 5;
    public static final int MANAGER_STOPPED = MANAGER_EVENT_ACTION_START_RANGE + 6;
    public static final int MANAGER_DISPOSING = MANAGER_EVENT_ACTION_START_RANGE + 7;
    public static final int MANAGER_DISPOSED = MANAGER_EVENT_ACTION_START_RANGE + 8;
    public static final int MANAGER_DISPOSING_CONNECTORS = MANAGER_EVENT_ACTION_START_RANGE + 9;
    public static final int MANAGER_DISPOSED_CONNECTORS = MANAGER_EVENT_ACTION_START_RANGE + 10;
    public static final int MANAGER_STARTING_MODELS = MANAGER_EVENT_ACTION_START_RANGE + 11;
    public static final int MANAGER_STARTED_MODELS = MANAGER_EVENT_ACTION_START_RANGE + 12;
    public static final int MANAGER_STOPPING_MODELS = MANAGER_EVENT_ACTION_START_RANGE + 13;
    public static final int MANAGER_STOPPED_MODELS = MANAGER_EVENT_ACTION_START_RANGE + 14;

    static {
        registerAction("initialising", MANAGER_INITIALISING);
        registerAction("initialised", MANAGER_INITIALISED);
        registerAction("starting", MANAGER_STARTING);
        registerAction("started", MANAGER_STARTED);
        registerAction("stopping", MANAGER_STOPPING);
        registerAction("stopped", MANAGER_STOPPED);
        registerAction("disposing", MANAGER_DISPOSING);
        registerAction("disposed", MANAGER_DISPOSED);
        registerAction("disposing connectors", MANAGER_DISPOSING_CONNECTORS);
        registerAction("disposed connectors", MANAGER_DISPOSED_CONNECTORS);
        registerAction("starting models", MANAGER_STARTING_MODELS);
        registerAction("started models", MANAGER_STARTED_MODELS);
        registerAction("stopping models", MANAGER_STOPPING_MODELS);
        registerAction("stopped models", MANAGER_STOPPED_MODELS);
    }

    private String clusterId;
    private String domain;


    public ManagerNotification(UMOManagementContext context, String action)
    {
        this(context, getActionId(action));
    }

    public ManagerNotification(UMOManagementContext context, int action)
    {
        super(getId(context), action);
        resourceIdentifier = getId(context);
        this.clusterId = context.getClusterId();
        this.domain = context.getDomain();
    }

    private static String getId(UMOManagementContext context)
    {
        return context.getDomain() + "." + context.getClusterId() + "." + context.getId();
    }

    public String getClusterId()
    {
        return clusterId;
    }

    public String getDomain()
    {
        return domain;
    }

    protected String getPayloadToString()
    {
        return ((UMOManagementContext) source).getId();
    }

    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier
                + ", timestamp=" + timestamp + "}";
    }
}
