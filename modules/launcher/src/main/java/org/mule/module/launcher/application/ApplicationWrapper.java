/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.application;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.io.IOException;

/**
 * Decorates the target deployer to properly switch out context classloader for deployment
 * one where applicable. E.g. init() phase may load custom classes for an application, which
 * must be executed with deployment (app) classloader in the context, and not Mule system
 * classloader.
 */
public class ApplicationWrapper implements Application
{

    private Application delegate;

    protected ApplicationWrapper(Application delegate) throws IOException
    {
        this.delegate = delegate;
    }

    public void dispose()
    {
        // moved wrapper logic into the actual implementation, as redeploy() invokes it directly, bypassing
        // classloader cleanup
        delegate.dispose();
    }

    public ClassLoader getDeploymentClassLoader()
    {
        return delegate.getDeploymentClassLoader();
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

    public String getAppName()
    {
        return delegate.getAppName();
    }

    public ApplicationDescriptor getDescriptor()
    {
        return delegate.getDescriptor();
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s)", getClass().getName(), delegate);
    }

    public Application getDelegate()
    {
        return delegate;
    }
}
