package org.mule.module.launcher;

import org.mule.api.MuleContext;

/**
 * Decorates the target deployer to properly switch out context classloader for deployment
 * one where applicable. E.g. init() phase may load custom classes for an application, which
 * must be executed with deployment (app) classloader in the context, and not Mule system
 * classloader.
 */
public class DeployerWrapper<M> implements Deployer<M>
{

    private Deployer<M> delegate;

    public DeployerWrapper(Deployer<M> delegate)
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
            Thread.currentThread().setContextClassLoader(appCl);
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
            Thread.currentThread().setContextClassLoader(appCl);
            delegate.install();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    public void restart()
    {
        delegate.restart();
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
            Thread.currentThread().setContextClassLoader(appCl);
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
            Thread.currentThread().setContextClassLoader(appCl);
            delegate.stop();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }
}
