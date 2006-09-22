/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.provider.UMOConnectable;

/**
 * Is fired by a connector when a connection is made, or disconnected of the connection
 * fails.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ConnectionNotification extends UMOServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6455441938378523145L;
    public static final int CONNECTION_CONNECTED = CONNECTION_EVENT_ACTION_START_RANGE + 1;
    public static final int CONNECTION_FAILED = CONNECTION_EVENT_ACTION_START_RANGE + 2;
    public static final int CONNECTION_DISCONNECTED = CONNECTION_EVENT_ACTION_START_RANGE + 3;

    private static final transient String[] ACTIONS = new String[] { "connected", "connect failed", "disconnected" };

    public ConnectionNotification(UMOConnectable resource, String identifier, int action)
    {
        super(resource, action);
        resourceIdentifier = identifier;
    }

    protected String getPayloadToString()
    {
        if (source instanceof UMOConnectable) {
            return ((UMOConnectable) source).getConnectionDescription();
        }
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

    public String getType() {
        if(action == CONNECTION_DISCONNECTED) {
            return TYPE_WARNING;
        }
        if(action == CONNECTION_FAILED) {
            return TYPE_ERROR;
        }
        return TYPE_INFO;
    }

}
