/*
 * $Id: DefaultMuleApplication.java 19667 2010-09-16 16:32:41Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.module.launcher.AbstractFileWatcher;
import org.mule.module.launcher.ApplicationMuleContextBuilder;
import org.mule.module.launcher.ConfigChangeMonitorThreadFactory;
import org.mule.module.launcher.DeploymentInitException;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.DeploymentStopException;
import org.mule.module.launcher.GoodCitizenClassLoader;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.util.ClassUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EmbeddedMuleApplication implements Application
{

    protected static final int DEFAULT_RELOAD_CHECK_INTERVAL_MS = 3000;

    protected transient final Log logger = LogFactory.getLog(getClass());

    protected ScheduledExecutorService watchTimer;

    private MuleContext muleContext;
    private ClassLoader deploymentClassLoader;
    private ApplicationDescriptor descriptor;

    public String getAppName()
    {
        return "Embedded";
    }

    public void install()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Installing application: " + getAppName());
        }

        descriptor = createApplicationDescriptor();
        deploymentClassLoader = createDeploymentClassLoader();
    }

    protected ApplicationDescriptor createApplicationDescriptor()
    {
        ApplicationDescriptor d = new ApplicationDescriptor();
        d.setDomain(getAppName());
        return d;
    }

    public ApplicationDescriptor getDescriptor()
    {
        return descriptor;
    }

    public void start()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Starting application: " + getAppName());
        }

        try
        {
            this.muleContext.start();
        }
        catch (MuleException e)
        {
            // TODO add app name to the exception field
            throw new DeploymentStartException(MessageFactory.createStaticMessage(getAppName()), e);
        }
    }

    public void init()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Initializing application: " + getAppName());
        }

        try
        {
            ConfigurationBuilder cfgBuilder = createConfigurationBuilder();

            if (!cfgBuilder.isConfigured())
            {
                //Load application properties first since they may be needed by other configuration builders
                List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>(2);

                addBuilders(builders);

                builders.add(cfgBuilder);

                this.muleContext = createMuleContext(builders);

                if (descriptor.isRedeploymentEnabled())
                {
                    final AbstractFileWatcher watcher = createRedeployMonitor();
                    if (watcher != null)
                    {
                        registerFileWatcher(watcher);
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new DeploymentInitException(CoreMessages.failedToLoad(getAppName()), e);
        }
    }

    protected void registerFileWatcher(final AbstractFileWatcher watcher) throws NotificationException
    {
        // register a config monitor only after context has started, as it may take some time
        muleContext.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
        {

            public void onNotification(MuleContextNotification notification)
            {
                final int action = notification.getAction();
                switch (action)
                {
                    case MuleContextNotification.CONTEXT_STARTED:
                        scheduleConfigMonitor(watcher);
                        break;
                    case MuleContextNotification.CONTEXT_STOPPING:
                        watchTimer.shutdownNow();
                        muleContext.unregisterListener(this);
                        break;
                }
            }
        });
    }

    protected void addBuilders(List<ConfigurationBuilder> builders)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
        InvocationTargetException
    {
        final Map<String,String> appProperties = descriptor.getAppProperties();

        //Add the app.home variable to the context
        appProperties.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, getAppHome());

        builders.add(new SimpleConfigurationBuilder(appProperties));

        // If the annotations module is on the classpath, add the annotations config builder to the list
        // This will enable annotations config for this instance
        //We need to add this builder before spring so that we can use Mule annotations in Spring or any other builder
        if (ClassUtils.isClassOnPath(MuleServer.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
        {
            Object configBuilder = ClassUtils.instanciateClass(
                MuleServer.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, ClassUtils.NO_ARGS, getClass());
            builders.add((ConfigurationBuilder) configBuilder);
        }
    }

    protected String getAppHome()
    {
        return new File(".").getAbsolutePath();
    }

    protected ConfigurationBuilder createConfigurationBuilder()
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
        InvocationTargetException
    {
        return new DefaultsConfigurationBuilder();
    }

    protected MuleContext createMuleContext(List<ConfigurationBuilder> builders)
        throws InitialisationException, ConfigurationException
    {
        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        return muleContextFactory.createMuleContext(builders, new ApplicationMuleContextBuilder(descriptor));
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public ClassLoader getDeploymentClassLoader()
    {
        return this.deploymentClassLoader;
    }

    public void dispose()
    {
        if (muleContext == null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("MuleContext not created, nothing to dispose of");
            }
            return;
        }

        if (muleContext.isStarted() && !muleContext.isDisposed())
        {
            stop();
        }
        
        if (logger.isInfoEnabled())
        {
            logger.info("Disposing application: " + getAppName());
        }

        muleContext.dispose();
        muleContext = null;
        // kill any refs to the old classloader to avoid leaks
        Thread.currentThread().setContextClassLoader(null);
    }

    public void redeploy()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Redeploying application: " + getAppName());
        }
        dispose();
        install();

        // update thread with the fresh new classloader just created during the install phase
        final ClassLoader cl = getDeploymentClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        init();
        start();

        // release the ref
        Thread.currentThread().setContextClassLoader(null);
    }

    public void stop()
    {
        if (this.muleContext == null)
        {
            // app never started, maybe due to a previous error
            return;
        }
        if (logger.isInfoEnabled())
        {
            logger.info("Stopping application: " + getAppName());
        }
        try
        {
            this.muleContext.stop();
        }
        catch (MuleException e)
        {
            // TODO add app name to the exception field
            throw new DeploymentStopException(MessageFactory.createStaticMessage(getAppName()), e);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             getAppName(),
                             Integer.toHexString(System.identityHashCode(this)));
    }

    protected ClassLoader createDeploymentClassLoader()
    {
        return new GoodCitizenClassLoader(new URL[0], getClass().getClassLoader());
    }

    protected AbstractFileWatcher createRedeployMonitor() throws NotificationException
    {
        return null;
    }

    protected void scheduleConfigMonitor(AbstractFileWatcher watcher)
    {
        final int reloadIntervalMs = DEFAULT_RELOAD_CHECK_INTERVAL_MS;
        watchTimer = Executors.newSingleThreadScheduledExecutor(new ConfigChangeMonitorThreadFactory(getAppName()));

        watchTimer.scheduleWithFixedDelay(watcher, reloadIntervalMs, reloadIntervalMs, TimeUnit.MILLISECONDS);

        if (logger.isInfoEnabled())
        {
            logger.info("Reload interval: " + reloadIntervalMs);
        }
    }

    protected class ConfigFileWatcher extends AbstractFileWatcher
    {
        public ConfigFileWatcher(File watchedResource)
        {
            super(watchedResource);
        }

        @Override
        protected synchronized void onChange(File file)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("================== Reloading " + file);
            }

            // grab the proper classloader for our context
            final ClassLoader cl = getDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            redeploy();
        }
    }
}
