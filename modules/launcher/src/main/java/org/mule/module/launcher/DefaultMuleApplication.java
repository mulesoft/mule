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
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.builders.AutoConfigurationBuilder;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Meta data is commandline options.
 */
public class DefaultMuleApplication implements Application<Map<String, Object>>
{

    protected static final int DEFAULT_RELOAD_CHECK_INTERVAL_MS = 3000;

    protected transient final Log logger = LogFactory.getLog(getClass());

    protected ScheduledExecutorService watchTimer;

    private String appName;
    private Map<String, Object> metaData;
    protected URL configUrl;
    private MuleContext muleContext;
    private ClassLoader deploymentClassLoader;
    private boolean redeploymentEnabled = true;
    protected ApplicationDescriptor descriptor;

    public DefaultMuleApplication(String appName)
    {
        this.appName = appName;
    }

    public void install()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Installing application: " + appName);
        }

        final String muleHome = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
        // try to load the config as a file as well
        final String configPath = String.format("%s/apps/%s/%s", muleHome, getAppName(), ApplicationDescriptor.DEFAULT_CONFIGURATION_URL);

        AppBloodhound bh = new DefaultAppBloodhound();
        try
        {
            descriptor = bh.fetch(getAppName());
        }
        catch (IOException e)
        {
            throw new InstallException(MessageFactory.createStaticMessage("Failed to parse the application deployment descriptor"), e);
        }

        configUrl = IOUtils.getResourceAsUrl(configPath, getClass(), true, false);
        if (configUrl == null)
        {
            //System.out.println(CoreMessages.configNotFoundUsage());
            // TODO a better message
            throw new InstallException(CoreMessages.configNotFoundUsage());
        }


        // Configuration builder
        String builder = (String) metaData.get("builder");
        if (StringUtils.isBlank(builder))
        {
            builder = ApplicationDescriptor.CLASSNAME_DEFAULT_CONFIG_BUILDER;
        }

        createDeploymentClassLoader();
    }

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public void start()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Starting application: " + appName);
        }

        try
        {
            this.muleContext.start();
        }
        catch (MuleException e)
        {
            // TODO add app name to the exception field
            throw new DeploymentStartException(MessageFactory.createStaticMessage(appName), e);
        }
    }

    public void init()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Initializing application: " + appName);
        }

        String configBuilderClassName = null;
        try
        {
            // Configuration builder
            // Provide a shortcut for Spring: "-builder spring"
            final String builderFromDesc = descriptor.getConfigurationBuilder();
            if ("spring".equalsIgnoreCase(builderFromDesc))
            {
                configBuilderClassName = ApplicationDescriptor.CLASSNAME_SPRING_CONFIG_BUILDER;
            }
            else if (builderFromDesc == null)
            {
                configBuilderClassName = AutoConfigurationBuilder.class.getName();
            }
            else
            {
                configBuilderClassName = builderFromDesc;
            }
            // create a new ConfigurationBuilder that is disposed afterwards
            ConfigurationBuilder cfgBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(configBuilderClassName,
                                                                                                 new Object[] {configUrl.toExternalForm()}, getDeploymentClassLoader());
            if (!cfgBuilder.isConfigured())
            {
                //List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>(2);
                //builders.add(cfgBuilder);

                // If the annotations module is on the classpath, add the annotations config builder to the list
                // This will enable annotations config for this instance
                //if (ClassUtils.isClassOnPath(CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
                //{
                //    Object configBuilder = ClassUtils.instanciateClass(
                //            CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, ClassUtils.NO_ARGS, getClass());
                //    builders.add((ConfigurationBuilder) configBuilder);
                //}

                DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
                // TODO properties for the app should come from the app descriptor
                this.muleContext = muleContextFactory.createMuleContext(cfgBuilder, new DefaultMuleContextBuilder()
                {
                    @Override
                    protected DefaultMuleConfiguration createMuleConfiguration()
                    {
                        final DefaultMuleConfiguration configuration = new DefaultMuleConfiguration(true);
                        configuration.setId(appName);
                        final String encoding = descriptor.getEncoding();
                        if (StringUtils.isNotBlank(encoding))
                        {
                            configuration.setDefaultEncoding(encoding);
                        }
                        return configuration;
                    }
                });

                if (redeploymentEnabled)
                {
                    createRedeployMonitor();
                }
            }
        }
        catch (Exception e)
        {
            throw new DeploymentInitException(CoreMessages.failedToLoad(configBuilderClassName), e);
        }
    }

    public void setMetaData(Map<String, Object> metaData)
    {
        this.metaData = metaData;
    }

    public Map<String, Object> getMetaData()
    {
        return this.metaData;
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
            logger.info("Disposing application: " + appName);
        }

        muleContext.dispose();
        // kill any refs to the old classloader to avoid leaks
        Thread.currentThread().setContextClassLoader(null);
    }

    public void redeploy()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Redeploying application: " + appName);
        }
        dispose();
        install();

        // update thread with the fresh new classloader just created during the install phase
        final ClassLoader cl = getDeploymentClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        init();
        start();
    }

    public void stop()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Stopping application: " + appName);
        }
        try
        {
            this.muleContext.stop();
        }
        catch (MuleException e)
        {
            // TODO add app name to the exception field
            throw new DeploymentStopException(MessageFactory.createStaticMessage(appName), e);
        }
    }

    public boolean isRedeploymentEnabled()
    {
        return redeploymentEnabled;
    }

    public void setRedeploymentEnabled(boolean redeploymentEnabled)
    {
        this.redeploymentEnabled = redeploymentEnabled;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             appName,
                             Integer.toHexString(System.identityHashCode(this)));
    }

    protected void createDeploymentClassLoader()
    {
        final String domain = descriptor.getDomain();
        ClassLoader parent;

        if (StringUtils.isBlank(domain) || DefaultMuleSharedDomainClassLoader.DEFAULT_DOMAIN_NAME.equals(domain))
        {
            parent = new DefaultMuleSharedDomainClassLoader(getClass().getClassLoader());
        }
        else
        {
            // TODO handle non-existing domains with an exception
            parent = new MuleSharedDomainClassLoader(domain, getClass().getClassLoader());
        }

        this.deploymentClassLoader = new MuleApplicationClassLoader(appName, new File(configUrl.getFile()), parent);
    }

    protected void createRedeployMonitor() throws NotificationException
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Monitoring for hot-deployment: " + configUrl.toExternalForm());
        }

        final FileWatcher watcher = new ConfigFileWatcher(new File(configUrl.getFile()));

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

    protected void scheduleConfigMonitor(FileWatcher watcher)
    {
        final int reloadIntervalMs = DEFAULT_RELOAD_CHECK_INTERVAL_MS;
        watchTimer = Executors.newSingleThreadScheduledExecutor(new ConfigChangeMonitorThreadFactory(appName));

        watchTimer.scheduleWithFixedDelay(watcher, reloadIntervalMs, reloadIntervalMs, TimeUnit.MILLISECONDS);

        if (logger.isInfoEnabled())
        {
            logger.info("Reload interval: " + reloadIntervalMs);
        }
    }


    protected class ConfigFileWatcher extends FileWatcher
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
