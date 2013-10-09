/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.ServerNotification;

/**
 * These notifications are fired when before and after a service component is
 * invoked.
 */
public class ComponentMessageNotification extends ServerNotification
{

    private static final long serialVersionUID = -6369685122731797646L;

    public static final int COMPONENT_PRE_INVOKE = COMPONENT_EVENT_ACTION_START_RANGE + 1;
    public static final int COMPONENT_POST_INVOKE = COMPONENT_EVENT_ACTION_START_RANGE + 2;

    protected transient FlowConstruct flowConstruct;
    protected transient Component component;

    static
    {
        registerAction("component pre invoke", COMPONENT_PRE_INVOKE);
        registerAction("component post invoke", COMPONENT_POST_INVOKE);
    }

    /**
     * @param message
     * @param action
     */
    public ComponentMessageNotification(MuleMessage message,
                                        Component component,
                                        FlowConstruct flowConstruct,
                                        int action)
    {
        super(cloneMessage(message), action);
        this.flowConstruct = flowConstruct;
        this.component = component;
        resourceIdentifier = flowConstruct.getName();

    }

    @Override
    protected String getPayloadToString()
    {
        return ((MuleMessage) source).getPayloadForLogging();
    }

    /**
     * @return the message
     */
    public String getServiceName()
    {
        return resourceIdentifier;
    }

    @Override
    public String toString()
    {
        return EVENT_NAME + "{action=" + getActionName(action) + ", message: " + source + ", resourceId="
               + resourceIdentifier + ", timestamp=" + timestamp + ", serverId=" + serverId + ", component: "
               + "}";
    }

    @Override
    public String getType()
    {
        return "trace";
    }

    @Override
    public MuleMessage getSource()
    {
        return (MuleMessage) super.getSource();
    }

    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    public Component getComponent()
    {
        return component;
    }
}
