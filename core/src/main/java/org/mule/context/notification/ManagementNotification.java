/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;

/**
 * <code>ManagementNotification</code> is fired when monitored resources such as
 * internal queues reach capacity
 * 
 */
public class ManagementNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -259130553709035786L;

    // TODO resource status notifications here i.e.
    public static final int MANAGEMENT_COMPONENT_QUEUE_EXHAUSTED = MANAGEMENT_EVENT_ACTION_START_RANGE + 1;
    public static final int MANAGEMENT_NODE_PING = MANAGEMENT_EVENT_ACTION_START_RANGE + 2;

    static {
        registerAction("service queue exhausted", MANAGEMENT_COMPONENT_QUEUE_EXHAUSTED);
        registerAction("node ping", MANAGEMENT_NODE_PING);
    }

    public ManagementNotification(Object message, int action)
    {
        super(message, action);
    }
}
