/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.core.context.notification.CustomNotification;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

/**
 * Custom notification that communicates the change of state in a {@link ConfigurationInstance}
 *
 * @since 4.0
 */
public class ConfigurationInstanceNotification extends CustomNotification
{

    private static final int CONFIGURATION_INSTANCE_ACTION_BASE = (CUSTOM_EVENT_ACTION_START_RANGE + 4) * 5;
    private static int ACTION_INDEX = 0;

    public static final int CONFIGURATION_STOPPED = ++ACTION_INDEX + CONFIGURATION_INSTANCE_ACTION_BASE;

    static
    {
        registerAction("Configuration instance is stopped", CONFIGURATION_STOPPED);
    }

    private final ConfigurationInstance configurationInstance;

    public ConfigurationInstanceNotification(ConfigurationInstance configurationInstance, int action)
    {
        super(null, action);
        this.configurationInstance = configurationInstance;
        this.action = action;
    }

    @Override
    public String toString()
    {
        return String.format("%s {action=%s, resourceId=%s, timestamp=%s}", EVENT_NAME, getActionName(action), resourceIdentifier, timestamp);
    }

    public ConfigurationInstance getConfigurationInstance()
    {
        return configurationInstance;
    }
}
