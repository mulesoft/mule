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
