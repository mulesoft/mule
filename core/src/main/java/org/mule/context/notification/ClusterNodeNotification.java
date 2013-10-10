/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
