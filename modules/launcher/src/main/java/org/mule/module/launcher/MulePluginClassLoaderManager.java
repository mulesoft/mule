/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MulePluginClassLoaderManager implements PluginClassLoaderManager
{

    private final List<ClassLoader> classLoaders = new LinkedList<ClassLoader>();

    @Override
    public void addPluginClassLoader(ClassLoader classLoader)
    {
        synchronized (classLoaders)
        {
            classLoaders.add(classLoader);
        }
    }

    @Override
    public List<ClassLoader> getPluginClassLoaders()
    {
        synchronized (classLoaders)
        {
            return Collections.unmodifiableList(classLoaders);
        }
    }
}
