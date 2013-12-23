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
        executeWithinArtifactClassLoader(new ArtifactAction()
        {
            @Override
            public void execute()
            {
                delegate.dispose();
            }
        });
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
        executeWithinArtifactClassLoader(new ArtifactAction()
        {
            @Override
            public void execute()
            {
                delegate.init();
            }
        });
    }

    public void install() throws InstallException
    {
        executeWithinArtifactClassLoader(new ArtifactAction()
        {
            @Override
            public void execute()
            {
                delegate.install();
            }
        });
    }

    @Override
    public String getArtifactName()
    {
        return delegate.getArtifactName();
    }

    @Override
    public File[] getResourceFiles()
    {
        return delegate.getResourceFiles();
    }

    public void start() throws DeploymentStartException
    {
        executeWithinArtifactClassLoader(new ArtifactAction()
        {
            @Override
            public void execute()
            {
                delegate.start();
            }
        });
    }

    public void stop()
    {
        executeWithinArtifactClassLoader(new ArtifactAction()
        {
            @Override
            public void execute()
            {
                delegate.stop();
            }
        });
    }

    private void executeWithinArtifactClassLoader(ArtifactAction artifactAction)
    {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try
        {
            if (getArtifactClassLoader() != null)
            {
                // if not initialized yet, it can be null
                ClassLoader artifactCl = getArtifactClassLoader().getClassLoader();
                if (artifactCl != null)
                {
                    Thread.currentThread().setContextClassLoader(artifactCl);
                }
            }
            artifactAction.execute();
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

    private interface ArtifactAction
    {
        void execute();
    }
}
