/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.registry.Registry;
import org.mule.umo.manager.UMOServerNotification;

/**
 * <code>RegistyNotification</code> is fired when an event such as an object being
 * registered in the Registry starting occurs. The payload of this event will always
 * be a reference to the Registry ID.
 *
 * @see org.mule.registry.Registry
 * @see org.mule.umo.UMOManagementContext
 */
public class RegistryNotification extends UMOServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3246036188021581121L;

    public static final int REGISTRY_INITIALISING = REGISTRY_EVENT_ACTION_START_RANGE + 1;
    public static final int REGISTRY_INITIALISED = REGISTRY_EVENT_ACTION_START_RANGE + 2;
    public static final int REGISTRY_DISPOSING = REGISTRY_EVENT_ACTION_START_RANGE + 3;
    public static final int REGISTRY_DISPOSED = REGISTRY_EVENT_ACTION_START_RANGE + 4;

    static {
        registerAction("initialising", REGISTRY_INITIALISING);
        registerAction("initialised", REGISTRY_INITIALISED);
        registerAction("disposing", REGISTRY_DISPOSING);
        registerAction("disposed", REGISTRY_DISPOSED);
    }

    public RegistryNotification(Registry registry, String action)
    {
        this(registry, getActionId(action));
    }

    public RegistryNotification(Registry registry, int action)
    {
        super(registry.getRegistryId(), action);
        resourceIdentifier = registry.getRegistryId();
    }


    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier
                + ", timestamp=" + timestamp + "}";
    }
}