/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;

/**
 * <code>FlowConstructNotification</code> is fired when an event such as the flow
 * construct starting occurs. The payload of this event will always be a reference to
 * the flow construct.
 */
public class FlowConstructNotification extends ServerNotification implements BlockingServerEvent
{
    private static final long serialVersionUID = 6658641434183647952L;
    public static final int FLOW_CONSTRUCT_INITIALISED = FLOW_CONSTRUCT_EVENT_ACTION_START_RANGE + 1;
    public static final int FLOW_CONSTRUCT_STARTED = FLOW_CONSTRUCT_EVENT_ACTION_START_RANGE + 2;
    public static final int FLOW_CONSTRUCT_STOPPED = FLOW_CONSTRUCT_EVENT_ACTION_START_RANGE + 3;
    public static final int FLOW_CONSTRUCT_PAUSED = FLOW_CONSTRUCT_EVENT_ACTION_START_RANGE + 4;
    public static final int FLOW_CONSTRUCT_RESUMED = FLOW_CONSTRUCT_EVENT_ACTION_START_RANGE + 5;
    public static final int FLOW_CONSTRUCT_DISPOSED = FLOW_CONSTRUCT_EVENT_ACTION_START_RANGE + 6;

    static
    {
        registerAction("flow construct initialised", FLOW_CONSTRUCT_INITIALISED);
        registerAction("flow construct started", FLOW_CONSTRUCT_STARTED);
        registerAction("flow construct stopped", FLOW_CONSTRUCT_STOPPED);
        registerAction("flow construct paused", FLOW_CONSTRUCT_PAUSED);
        registerAction("flow construct resumed", FLOW_CONSTRUCT_RESUMED);
        registerAction("flow construct disposed", FLOW_CONSTRUCT_DISPOSED);
    }

    public FlowConstructNotification(FlowConstruct flowConstruct, int action)
    {
        super(flowConstruct.getName(), action);
        resourceIdentifier = flowConstruct.getName();
    }
}
