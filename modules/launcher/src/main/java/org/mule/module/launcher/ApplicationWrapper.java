/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.api.MuleContext;

/**
 * Decorates the target deployer to properly switch out context classloader for deployment
 * one where applicable. E.g. init() phase may load custom classes for an application, which
 * must be executed with deployment (app) classloader in the context, and not Mule system
 * classloader.
 */
public class ApplicationWrapper<M> implements Application<M>
{

    private Application<M> delegate;

    public ApplicationWrapper(Application<M> delegate)
    {
        this.delegate = delegate;
    }

    public void dispose()
    {
        delegate.dispose();
    }

    public ClassLoader getDeploymentClassLoader()
    {
        return delegate.getDeploymentClassLoader();
    }

    public M getMetaData()
    {
        return delegate.getMetaData();
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
            ClassLoader appCl = getDeploymentClassLoader();
            // if not initialized yet, it can be null
            if (appCl != null)
            {
                Thread.currentThread().setContextClassLoader(appCl);
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
            ClassLoader appCl = getDeploymentClassLoader();
            // if not initialized yet, it can be null
            if (appCl != null)
            {
                Thread.currentThread().setContextClassLoader(appCl);
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

    public void setMetaData(M metaData)
    {
        delegate.setMetaData(metaData);
    }

    public void start() throws DeploymentStartException
    {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try
        {
            ClassLoader appCl = getDeploymentClassLoader();
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
            ClassLoader appCl = getDeploymentClassLoader();
            // if not initialized yet, it can be null
            if (appCl != null)
            {
                Thread.currentThread().setContextClassLoader(appCl);
            }
            delegate.stop();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s)@%s", getClass().getName(),
                             delegate,
                             Integer.toHexString(System.identityHashCode(this)));

    }
}
