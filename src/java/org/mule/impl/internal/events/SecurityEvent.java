/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.internal.events;

import org.mule.umo.UMOServerEvent;

/**
 * <code>SecurityEvent</code> is fired when a request for authorisation occurs.
 * The event may denote successful accues or denied access depending on the type
 * of event.  Subscribing to these events developers can maintain an access log,
 * block clients, etc.
 *
 * @see org.mule.MuleManager
 * @see org.mule.umo.UMOManager
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SecurityEvent extends UMOServerEvent
{
    //todo add security actions here

    private String[] actions = new String[]{};
    public SecurityEvent(Object message, int action)
    {
        super(message, action);
    }

    protected String getActionName(int action)
    {
        int i = action - COMPONENT_EVENT_ACTION_START_RANGE;
        if(i-1 > actions.length) return String.valueOf(action);
        return actions[i-1];
    }
}
