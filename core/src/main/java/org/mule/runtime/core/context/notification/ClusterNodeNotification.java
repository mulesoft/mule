/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;

/**
 * Notifies when there are a cluster node event
 */
public class ClusterNodeNotification extends ServerNotification implements BlockingServerEvent
{

    public static final int PRIMARY_CLUSTER_NODE_SELECTED = CLUSTER_NODE_EVENT_ACTION_START_RANGE + 1;

    static
    {
        registerAction("cluster node selected as primary", PRIMARY_CLUSTER_NODE_SELECTED);
    }

    public ClusterNodeNotification(Object message, int action)
    {
        super(message, action);
    }
}
