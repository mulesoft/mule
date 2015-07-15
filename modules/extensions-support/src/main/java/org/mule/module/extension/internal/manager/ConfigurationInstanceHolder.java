/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

/**
 * Component to hold a configuration's instance and its name.
 *
 * @since 3.8.0
 */
final class ConfigurationInstanceHolder
{

    private final String name;
    private final Object configurationInstance;

    public ConfigurationInstanceHolder(String instanceName, Object configurationInstance)
    {
        this.name = instanceName;
        this.configurationInstance = configurationInstance;
    }

    public Object getConfigurationInstance()
    {
        return configurationInstance;
    }

    public String getName()
    {
        return name;
    }
}
