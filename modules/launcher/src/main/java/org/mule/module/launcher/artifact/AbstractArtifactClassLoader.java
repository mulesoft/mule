/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.module.launcher.DirectoryResourceLocator;
import org.mule.module.launcher.FineGrainedControlClassLoader;
import org.mule.module.launcher.LocalResourceLocator;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract implementation of the ArtifactClassLoader interface, that manages shutdown listeners.
 */
public abstract class AbstractArtifactClassLoader extends FineGrainedControlClassLoader implements ArtifactClassLoader
{
    private static final String DEFAULT_LEAK_CLEANER_CLASS = "DefaultLeakCleaner.class";

    protected Log logger = LogFactory.getLog(getClass());

    protected List<ShutdownListener> shutdownListeners = new ArrayList<ShutdownListener>();

    private LocalResourceLocator localResourceLocator;

    private LeakCleaner leakCleaner;

    private String leakCleanerClassName;

    public AbstractArtifactClassLoader(URL[] urls, ClassLoader parent)
    {
        this(urls, parent, Collections.<String>emptySet());
    }

    public AbstractArtifactClassLoader(URL[] urls, ClassLoader parent, Set<String> overrides)
    {
        super(urls, parent, overrides);
    }

    @Override
    public void addShutdownListener(ShutdownListener listener)
    {
        this.shutdownListeners.add(listener);
    }

    @Override
    public void dispose()
    {
        for (ShutdownListener listener : shutdownListeners)
        {
            try
            {
                listener.execute();
            }
            catch (Exception e)
            {
                logger.error(e);
            }
        }

        LeakCleaner leakCleaner = getLeakCleanerInstance();
        if (leakCleaner != null)
        {
            leakCleaner.clean();
        }

        super.dispose();
    }

    public String getLeakCleanerClassName()
    {
        return StringUtils.defaultString(leakCleanerClassName, DEFAULT_LEAK_CLEANER_CLASS);
    }

    public void setLeakCleanerClassName(String leakCleanerClass)
    {
        this.leakCleanerClassName = leakCleanerClass;
        leakCleaner = null;
    }

    public LeakCleaner getLeakCleanerInstance()
    {
        if(leakCleaner==null)
        {
            leakCleaner = createLeakCleanerInstance(getLeakCleanerClassName());
        }
        return leakCleaner;
    }

    private LeakCleaner createLeakCleanerInstance(String className)
    {
        InputStream classStream = this.getClass().getResourceAsStream(className);
        if (classStream == null)
        {
            classStream = AbstractArtifactClassLoader.class.getResourceAsStream(className);
        }
        if (classStream == null)
        {
            logger.warn("Could not find leak cleaner class with name: " + className);
            return null;
        }

        byte[] classBytes = IOUtils.toByteArray(classStream);
        Class clazz = this.defineClass(null, classBytes, 0, classBytes.length);
        try
        {
            return (LeakCleaner) clazz.newInstance();
        }
        catch (Exception e)
        {
            logger.warn("Could not instantiate leak cleaner instance of type: " + className, e);
        }
        return null;
    }

    public URL findLocalResource(String resourceName)
    {
        URL resource = getLocalResourceLocator().findLocalResource(resourceName);
        if (resource == null && getParent() instanceof LocalResourceLocator)
        {
            resource = ((LocalResourceLocator) getParent()).findLocalResource(resourceName);
        }
        return resource;
    }

    private LocalResourceLocator getLocalResourceLocator()
    {
        if (localResourceLocator == null)
        {
            localResourceLocator = new DirectoryResourceLocator(getLocalResourceLocations());
        }
        return localResourceLocator;
    }

    protected abstract String[] getLocalResourceLocations();
}
