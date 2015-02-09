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

    private static final String DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION = "/org/mule/module/launcher/artifact/DefaultResourceReleaser.class";

    protected Log logger = LogFactory.getLog(getClass());

    protected List<ShutdownListener> shutdownListeners = new ArrayList<ShutdownListener>();

    private LocalResourceLocator localResourceLocator;

    private String resourceReleaserClassLocation = DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION;

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

        try
        {
            createResourceReleaserInstance().release();
        }
        catch (Exception e)
        {
            logger.error(e);
        }

        super.dispose();
    }

    public void setResourceReleaserClassLocation(String resourceReleaserClassLocation)
    {
        this.resourceReleaserClassLocation = resourceReleaserClassLocation;
    }

    protected ResourceReleaser createResourceReleaserInstance()
    {
        try
        {
            InputStream classStream = this.getClass().getResourceAsStream(resourceReleaserClassLocation);
            byte[] classBytes = IOUtils.toByteArray(classStream);
            Class clazz = this.defineClass(null, classBytes, 0, classBytes.length);
            return (ResourceReleaser) clazz.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Can not create resource releaser instance from resource: " + resourceReleaserClassLocation, e);
        }
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
