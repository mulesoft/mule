/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.internal.events;

import org.mule.umo.manager.UMOServerEvent;

/**
 * <code>CustomEvent</code> Custom events can be used by components and other objects such as routers,
 * transformers, agents, etc to communicate a change of state to each other.
 * The Action value for the event is abitary. However care should be taken not to set the
 * action code to an existing action code. To ensure this doesn't happen always
 * set the action code greater than the CUSTOM_ACTION_START_RANGE.
 *
 * @see CustomEventListener
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CustomEvent extends UMOServerEvent
{
    /**
     * Creates a custom action event
     * @param message the message to associate with the event
     * @param action the action code for the event
     * @throws IllegalArgumentException if the action value is less than CUSTOM_ACTION_START_RANGE
     */
    public CustomEvent(Object message, int action)
    {
        super(message, action);
        if(action < CUSTOM_EVENT_ACTION_START_RANGE) {
            throw new IllegalArgumentException("Action range must be greater than CUSTOM_ACTION_START_RANGE (" + CUSTOM_EVENT_ACTION_START_RANGE + ")");
        }
    }

    protected String getActionName(int action)
    {
        int i = action - CUSTOM_EVENT_ACTION_START_RANGE;
        if(i-1 > getActionNames().length) return String.valueOf(action);
        return getActionNames()[i-1];
    }

    protected String[] getActionNames() {
        return new String[]{};
    }
}
