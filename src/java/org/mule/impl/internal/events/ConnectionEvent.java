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
 *
 */
package org.mule.impl.internal.events;

import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.manager.UMOServerEvent;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ConnectionEvent extends UMOServerEvent
{
    public static final int CONNECTION_CONNECTED = CONNECTION_EVENT_ACTION_START_RANGE + 1;
    public static final int CONNECTION_FAILED = CONNECTION_EVENT_ACTION_START_RANGE + 2;
    public static final int CONNECTION_DISCONNECTED = CONNECTION_EVENT_ACTION_START_RANGE + 3;

    private static final transient String[] ACTIONS = new String[] { "connected", "connect failed", "disconnected" };

    public ConnectionEvent(UMOEndpointURI message, int action)
    {
        super(message, action);
        resourceIdentifier = message.toString();
    }

    protected String getPayloadToString()
    {
        return source.toString();
    }

    protected String getActionName(int action)
    {
        int i = action - CONNECTION_EVENT_ACTION_START_RANGE;
        if (i - 1 > ACTIONS.length) {
            return String.valueOf(action);
        }
        return ACTIONS[i - 1];
    }
}
