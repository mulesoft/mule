/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ServerNotification;

/**
 * <code>CustomNotification</code> Custom notifications can be used by components
 * and other objects such as routers, transformers, agents, etc to communicate a
 * change of state to each other. The Action value for the event is abitary. However
 * care should be taken not to set the action code to an existing action code. To
 * ensure this doesn't happen always set the action code greater than the
 * CUSTOM_ACTION_START_RANGE.
 * 
 * @see CustomNotificationListener
 */
public class CustomNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 762448139858484536L;

    /**
     * Creates a custom action event
     * 
     * @param message the message to associate with the event
     * @param action the action code for the event
     * @throws IllegalArgumentException if the action value is less than
     *             CUSTOM_ACTION_START_RANGE
     */
    public CustomNotification(Object message, int action)
    {
        super(message, action);
        if (action < CUSTOM_EVENT_ACTION_START_RANGE && action > 0)
        {
            throw new IllegalArgumentException(
                "Action range must be greater than CUSTOM_ACTION_START_RANGE ("
                                + CUSTOM_EVENT_ACTION_START_RANGE + ")");
        }
    }

    public CustomNotification(Object message, int action, String resourceId)
    {
        super(message, action, resourceId);
        if (action < CUSTOM_EVENT_ACTION_START_RANGE && action > 0)
        {
            throw new IllegalArgumentException(
                "Action range must be greater than CUSTOM_ACTION_START_RANGE ("
                                + CUSTOM_EVENT_ACTION_START_RANGE + ")");
        }
    }

    protected String[] getActionNames()
    {
        return new String[]{};
    }
}
