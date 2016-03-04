/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.module.artifact.classloader.CompositeClassLoader;
import org.mule.module.artifact.classloader.DisposableClassLoader;
import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.artifact.classloader.ArtifactClassLoader;
import org.mule.module.artifact.classloader.ShutdownListener;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines a composite classloader for applications
 */
public class CompositeApplicationClassLoader extends CompositeClassLoader implements ApplicationClassLoader
{

    protected static final Log logger = LogFactory.getLog(CompositeApplicationClassLoader.class);

    private final String appName;

    public CompositeApplicationClassLoader(String appName, List<ClassLoader> classLoaders)
    {
        super(classLoaders);
        this.appName = appName;
    }

    @Override
    public String getAppName()
    {
        return appName;
    }

    @Override
    public String getArtifactName()
    {
        return this.appName;
    }

    @Override
    public URL findResource(String name)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            URL resource = findResource(classLoader, name);

            if (resource != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Resource '%s' loaded from classLoader '%s", name, classLoader));
                }

                return resource;
            }
        }

        return null;
    }

    private URL findResource(ClassLoader classLoader, String name)
    {
        try
        {
            Method findResourceMethod = findDeclaredMethod(classLoader, "findResource", String.class);

            return (URL) findResourceMethod.invoke(classLoader, name);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logReflectionLoadingError(name, classLoader, e, "Resource");
            }
        }

        return null;
    }

    @Override
    public URL findLocalResource(String resourceName)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            if( classLoader instanceof ArtifactClassLoader )
            {
                URL resource = ((ArtifactClassLoader)classLoader).findLocalResource(resourceName);
                if( resource!=null )
                {
                    return resource;
                }
            }
        }
        return null;
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return this;
    }

    @Override
    public void dispose()
    {
        for (ClassLoader classLoader : classLoaders)
        {
            if (classLoader instanceof DisposableClassLoader)
            {
                ((DisposableClassLoader) classLoader).dispose();
            }
        }
    }

    @Override
    public void addShutdownListener(ShutdownListener listener)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            if (classLoader instanceof MuleApplicationClassLoader)
            {
                ((ApplicationClassLoader)classLoader).addShutdownListener(listener);
                return;
            }
        }
    }

}
