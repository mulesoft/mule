/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.registry.Registry;

/**
 * <code>RegistryNotification</code> is fired when an event such as a Registry
 * being started occurs. The payload of this event will always
 * be a reference to the Registry ID.
 *
 * @see org.mule.api.registry.Registry
 * @see org.mule.api.MuleContext
 */
public class RegistryNotification extends ServerNotification
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
        registerAction("registry initialising", REGISTRY_INITIALISING);
        registerAction("registry initialised", REGISTRY_INITIALISED);
        registerAction("registry disposing", REGISTRY_DISPOSING);
        registerAction("registry disposed", REGISTRY_DISPOSED);
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


    @Override
    public String toString()
    {
        return EVENT_NAME + "{" + "action=" + getActionName(action) + ", resourceId=" + resourceIdentifier
                + ", timestamp=" + timestamp + "}";
    }
}
