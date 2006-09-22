/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.space;

import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.space.UMOSpace;

/**
 * Events that occur in UMOSpaces
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SpaceMonitorNotification extends UMOServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8723048757928555644L;
    public static final int SPACE_CREATED = SPACE_EVENT_ACTION_START_RANGE + 1;
    public static final int SPACE_ITEM_ADDED = SPACE_EVENT_ACTION_START_RANGE + 2;
    public static final int SPACE_ITEM_REMOVED = SPACE_EVENT_ACTION_START_RANGE + 3;
    public static final int SPACE_ITEM_EXPIRED = SPACE_EVENT_ACTION_START_RANGE + 4;
    public static final int SPACE_ITEM_MISS = SPACE_EVENT_ACTION_START_RANGE + 5;
    public static final int SPACE_DISPOSED = SPACE_EVENT_ACTION_START_RANGE + 6;
    public static final int SPACE_LISTENER_ADDED = SPACE_EVENT_ACTION_START_RANGE + 7;
    public static final int SPACE_LISTENER_REMOVED = SPACE_EVENT_ACTION_START_RANGE + 8;

    private static final transient String[] ACTIONS = new String[] { "created", "item added", "item removed", "item expired",
            "item miss", "disposed", "listener added", "listener removed"};

    private Object item;

    public SpaceMonitorNotification(UMOSpace space, int action, Object item)
    {
        super(space, action);
        resourceIdentifier = space.getName();
        this.item = item;
    }

    public Object getItem() {
        return item;
    }

    protected String getPayloadToString()
    {
        return ((UMOSpace) source).getName();
    }

    protected String getActionName(int action)
    {
        int i = action - SPACE_EVENT_ACTION_START_RANGE;
        if (i - 1 > ACTIONS.length) {
            return String.valueOf(action);
        }
        return ACTIONS[i - 1];
    }
}
