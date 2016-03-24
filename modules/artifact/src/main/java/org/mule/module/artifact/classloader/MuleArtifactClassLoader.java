/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.mule.module.artifact.classloader.ClassLoaderLookupPolicy.NULL_LOOKUP_POLICY;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract implementation of the ArtifactClassLoader interface, that manages shutdown listeners.
 */
public class MuleArtifactClassLoader extends FineGrainedControlClassLoader implements ArtifactClassLoader
{

    private static final String DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION = "/org/mule/module/artifact/classloader/DefaultResourceReleaser.class";
    private final String name;

    protected Log logger = LogFactory.getLog(getClass());

    protected List<ShutdownListener> shutdownListeners = new ArrayList<>();

    private LocalResourceLocator localResourceLocator;

    private String resourceReleaserClassLocation = DEFAULT_RESOURCE_RELEASER_CLASS_LOCATION;

    public MuleArtifactClassLoader(String name, URL[] urls, ClassLoader parent)
    {
        this(name, urls, parent, NULL_LOOKUP_POLICY);
    }

    public MuleArtifactClassLoader(String name, URL[] urls, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy)
    {
        super(urls, parent, lookupPolicy);
        checkArgument(!StringUtils.isEmpty(name), "Artifact name cannot be empty");
        this.name = name;
    }

    @Override
    public String getArtifactName()
    {
        return name;
    }

    protected String[] getLocalResourceLocations()
    {
        return new String[0];
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return this;
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

    @Override
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
}
