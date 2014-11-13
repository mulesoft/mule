/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.bootstrap;

import org.mule.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

public class MuleBootstrapPropertiesService implements BootstrapPropertiesService
{

    private final Properties properties;

    public MuleBootstrapPropertiesService(Properties properties)
    {
        this.properties = properties;
    }

    @Override
    public Properties getProperties()
    {
        return properties;
    }

    @Override
    public Object instantiateClass(String name, Object... constructorArgs) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        return ClassUtils.instanciateClass(name, constructorArgs);
    }

    @Override
    public Class forName(String name) throws ClassNotFoundException
    {
        return Class.forName(name);
    }
}
