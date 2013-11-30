/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;

import java.io.File;
import java.io.IOException;

/**
 * Decorates the target deployer to properly switch out context classloader for deployment
 * one where applicable. E.g. init() phase may load custom classes for an application, which
 * must be executed with deployment (app) classloader in the context, and not Mule system
 * classloader.
 */
public class ArtifactWrapper<T extends Artifact> implements Artifact
{

    private T delegate;

    protected ArtifactWrapper(T artifact) throws IOException
    {
        this.delegate = artifact;
    }

    public void dispose()
    {
        // moved wrapper logic into the actual implementation, as redeploy() invokes it directly, bypassing
        // classloader cleanup
        delegate.dispose();
    }

    @Override
    public ArtifactClassLoader getArtifactClassLoader()
    {
        return delegate.getArtifactClassLoader();
    }

    public MuleContext getMuleContext()
    {
        return delegate.getMuleContext();
    }

    public void init()
    {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try
        {
            ClassLoader artifactCl = getArtifactClassLoader().getClassLoader();
            // if not initialized yet, it can be null
            if (artifactCl != null)
            {
                Thread.currentThread().setContextClassLoader(artifactCl);
            }
            delegate.init();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    public void install() throws InstallException
    {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try
        {

            ArtifactClassLoader artifactClassLoader = getArtifactClassLoader();
            if (artifactClassLoader != null)
            {
                ClassLoader appCl = artifactClassLoader.getClassLoader();
                // if not initialized yet, it can be null
                if (appCl != null)
                {
                    Thread.currentThread().setContextClassLoader(appCl);
                }
            }
            delegate.install();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    public void redeploy()
    {
        delegate.redeploy();
    }

    @Override
    public String getArtifactName()
    {
        return delegate.getArtifactName();
    }

    @Override
    public File[] getConfigResourcesFile()
    {
        return delegate.getConfigResourcesFile();
    }

    public void start() throws DeploymentStartException
    {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try
        {
            ClassLoader appCl = getArtifactClassLoader().getClassLoader();
            // if not initialized yet, it can be null
            if (appCl != null)
            {
                Thread.currentThread().setContextClassLoader(appCl);
            }
            delegate.start();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    public void stop()
    {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try
        {
            ArtifactClassLoader artifactClassLoader = getArtifactClassLoader();
            if (artifactClassLoader != null)
            {
                ClassLoader appCl = artifactClassLoader.getClassLoader();
                if (appCl != null)
                {
                    Thread.currentThread().setContextClassLoader(appCl);
                }
            }
            delegate.stop();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    public String getAppName()
    {
        return getArtifactName();
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s)", getClass().getName(), delegate);
    }

    public T getDelegate()
    {
        return delegate;
    }
}
