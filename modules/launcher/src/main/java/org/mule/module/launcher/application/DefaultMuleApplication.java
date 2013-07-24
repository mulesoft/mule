/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import static org.mule.util.SplashScreen.miniSplash;
import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.builders.AutoConfigurationBuilder;
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
import org.mule.module.launcher.DeploymentListener;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.DeploymentStopException;
import org.mule.module.launcher.InstallException;
import org.mule.module.launcher.MuleDeploymentService;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.ClassUtils;
import org.mule.util.ExceptionUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleApplication implements Application
{
    protected static final int DEFAULT_RELOAD_CHECK_INTERVAL_MS = 3000;
    protected static final String ANCHOR_FILE_BLURB = "Delete this file while Mule is running to undeploy this app in a clean way.";

    protected transient final Log logger = LogFactory.getLog(getClass());
    protected transient final Log deployLogger = LogFactory.getLog(MuleDeploymentService.class);

    protected ScheduledExecutorService watchTimer;

    protected MuleContext muleContext;
    protected ClassLoader deploymentClassLoader;
    protected ApplicationDescriptor descriptor;
    protected final ApplicationClassLoaderFactory applicationClassLoaderFactory;

    protected String[] absoluteResourcePaths;

    protected DeploymentListener deploymentListener;

    protected DefaultMuleApplication(ApplicationDescriptor appDesc, ApplicationClassLoaderFactory applicationClassLoaderFactory)
    {
        this.descriptor = appDesc;
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.deploymentListener = new NullDeploymentListener();
    }

    public void setDeploymentListener(DeploymentListener deploymentListener)
    {
        if (deploymentListener == null)
        {
            throw new IllegalArgumentException("Deployment listener cannot be null");
        }

        this.deploymentListener = deploymentListener;
    }

    @Override
    public void install()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("New app '%s'", descriptor.getAppName())));
        }

        // convert to absolute paths
        final String[] configResources = descriptor.getConfigResources();
        absoluteResourcePaths = new String[configResources.length];
        for (int i = 0; i < configResources.length; i++)
        {
            String resource = configResources[i];
            final File file = toAbsoluteFile(resource);
            if (!file.exists())
            {
                String message = String.format("Config for app '%s' not found: %s", getAppName(), file);
                throw new InstallException(MessageFactory.createStaticMessage(message));
            }

            absoluteResourcePaths[i] = file.getAbsolutePath();
        }

        deploymentClassLoader = applicationClassLoaderFactory.create(descriptor);
    }

    @Override
    public String getAppName()
    {
        return descriptor.getAppName();
    }

    @Override
    public ApplicationDescriptor getDescriptor()
    {
        return descriptor;
    }

    public void setAppName(String appName)
    {
        this.descriptor.setAppName(appName);
    }

    @Override
    public void start()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Starting app '%s'", descriptor.getAppName())));
        }

        try
        {
            this.muleContext.start();
            // save app's state in the marker file
            File marker = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), String.format("%s-anchor.txt", getAppName()));
            FileUtils.writeStringToFile(marker, ANCHOR_FILE_BLURB);

            // null CCL ensures we log at 'system' level
            // TODO create a more usable wrapper for any logger to be logged at sys level
            final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader(null);
                deployLogger.info(miniSplash(String.format("Started app '%s'", descriptor.getAppName())));
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(oldCl);
            }
        }
        catch (MuleException e)
        {
            // log it here so it ends up in app log, sys log will only log a message without stacktrace
            logger.error(null, ExceptionUtils.getRootCause(e));
            // TODO add app name to the exception field
            throw new DeploymentStartException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
        }
        catch (IOException e)
        {
            // log it here so it ends up in app log, sys log will only log a message without stacktrace
            logger.error(null, ExceptionUtils.getRootCause(e));
            // TODO add app name to the exception field
            throw new DeploymentStartException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
        }
    }

    @Override
    public void init()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Initializing app '%s'", descriptor.getAppName())));
        }

        try
        {
            ConfigurationBuilder cfgBuilder = createConfigurationBuilder();
            if (!cfgBuilder.isConfigured())
            {
                List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>(3);
                builders.add(createConfigurationBuilderFromApplicationProperties());

                // We need to add this builder before spring so that we can use Mule annotations in Spring or any other builder
                addAnnotationsConfigBuilderIfPresent(builders);

                builders.add(cfgBuilder);

                DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
                if (deploymentListener != null)
                {
                    muleContextFactory.addListener(new MuleContextDeploymentListener(getAppName(), deploymentListener));
                }
                this.muleContext = muleContextFactory.createMuleContext(builders, new ApplicationMuleContextBuilder(descriptor));

                if (descriptor.isRedeploymentEnabled())
                {
                    createRedeployMonitor();
                }
            }
        }
        catch (Exception e)
        {
            // log it here so it ends up in app log, sys log will only log a message without stacktrace
            logger.error(null, ExceptionUtils.getRootCause(e));
            throw new DeploymentInitException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
        }
    }

    protected ConfigurationBuilder createConfigurationBuilder() throws Exception
    {
        String configBuilderClassName = determineConfigBuilderClassName();
        return (ConfigurationBuilder) ClassUtils.instanciateClass(configBuilderClassName,
            new Object[] { absoluteResourcePaths }, getDeploymentClassLoader());
    }

    protected String determineConfigBuilderClassName()
    {
        // Provide a shortcut for Spring: "-builder spring"
        final String builderFromDesc = descriptor.getConfigurationBuilder();
        if ("spring".equalsIgnoreCase(builderFromDesc))
        {
            return ApplicationDescriptor.CLASSNAME_SPRING_CONFIG_BUILDER;
        }
        else if (builderFromDesc == null)
        {
            return AutoConfigurationBuilder.class.getName();
        }
        else
        {
            return builderFromDesc;
        }
    }

    protected ConfigurationBuilder createConfigurationBuilderFromApplicationProperties()
    {
        // Load application properties first since they may be needed by other configuration builders
        final Map<String,String> appProperties = descriptor.getAppProperties();

        // Add the app.home variable to the context
        File appPath = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), getAppName());
        appProperties.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, appPath.getAbsolutePath());

        appProperties.put(MuleProperties.APP_NAME_PROPERTY, getAppName());

        return new SimpleConfigurationBuilder(appProperties);
    }

    protected void addAnnotationsConfigBuilderIfPresent(List<ConfigurationBuilder> builders) throws Exception
    {
        // If the annotations module is on the classpath, add the annotations config builder to
        // the list. This will enable annotations config for this instance.
        if (ClassUtils.isClassOnPath(MuleServer.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
        {
            Object configBuilder = ClassUtils.instanciateClass(
                MuleServer.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, ClassUtils.NO_ARGS, getClass());
            builders.add((ConfigurationBuilder) configBuilder);
        }
    }

    @Override
    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public ClassLoader getDeploymentClassLoader()
    {
        return this.deploymentClassLoader;
    }

    @Override
    public void dispose()
    {
        // moved wrapper logic into the actual implementation, as redeploy() invokes it directly, bypassing
        // classloader cleanup
        try
        {
            ClassLoader appCl = getDeploymentClassLoader();
            // if not initialized yet, it can be null
            if (appCl != null)
            {
                Thread.currentThread().setContextClassLoader(appCl);
            }

            doDispose();

            if (appCl != null)
            {
                // close classloader to release jar connections in lieu of Java 7's ClassLoader.close()
                if (appCl instanceof Closeable)
                {
                    Closeable classLoader = (Closeable) appCl;
                    try
                    {
                        classLoader.close();
                    }
                    catch (IOException e)
                    {
                        // Ignore
                    }
                }
            }
        }
        finally
        {
            // kill any refs to the old classloader to avoid leaks
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @Override
    public void redeploy()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Redeploying app '%s'", descriptor.getAppName())));
        }

        String appName = getAppName();

        deploymentListener.onUndeploymentStart(appName);
        try
        {
            dispose();

            deploymentListener.onUndeploymentSuccess(appName);
        }
        catch (RuntimeException e)
        {
            deploymentListener.onUndeploymentFailure(appName, e);

            throw e;
        }

        install();

        // update thread with the fresh new classloader just created during the install phase
        final ClassLoader cl = getDeploymentClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        deploymentListener.onDeploymentStart(appName);
        try
        {
            init();
            start();

            deploymentListener.onDeploymentSuccess(appName);
        }
        catch(Throwable cause)
        {
            logger.error("Application deployment error", cause);
            deploymentListener.onDeploymentFailure(appName, cause);
        }

        // release the ref
        Thread.currentThread().setContextClassLoader(null);
    }

    @Override
    public void stop()
    {
        if (this.muleContext == null)
        {
            // app never started, maybe due to a previous error
            if (logger.isInfoEnabled())
            {
                logger.info(String.format("Stopping app '%s' with no mule context", descriptor.getAppName()));
            }
            return;
        }

        muleContext.getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);

        try
        {
            if (logger.isInfoEnabled())
            {
                logger.info(miniSplash(String.format("Stopping app '%s'", descriptor.getAppName())));
            }

            this.muleContext.stop();
        }
        catch (MuleException e)
        {
            // TODO add app name to the exception field
            throw new DeploymentStopException(MessageFactory.createStaticMessage(descriptor.getAppName()), e);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             descriptor.getAppName(),
                             Integer.toHexString(System.identityHashCode(this)));
    }

    protected void doDispose()
    {
        if (muleContext == null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info(String.format("App '%s' never started, nothing to dispose of", descriptor.getAppName()));
            }
            return;
        }

        if (muleContext.isStarted() && !muleContext.isDisposed())
        {
            try
            {
                stop();
            }
            catch (DeploymentStopException e)
            {
                // catch the stop errors and just log, we're disposing of an app anyway
                logger.error(e);
            }
        }
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Disposing app '%s'", descriptor.getAppName())));
        }

        muleContext.dispose();
        muleContext = null;
    }

    protected void createRedeployMonitor() throws NotificationException
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Monitoring for hot-deployment: " + StringUtils.join(absoluteResourcePaths, ';'));
        }

        List<File> configFiles = new ArrayList<File>();
        for (String path : absoluteResourcePaths)
        {
            configFiles.add(new File(path));
        }

        // register a config monitor only after context has started, as it may take some time
        new ConfigFileWatcher(configFiles).register();
    }

    protected void scheduleConfigMonitor(AbstractFileWatcher watcher)
    {
        final int reloadIntervalMs = DEFAULT_RELOAD_CHECK_INTERVAL_MS;
        watchTimer = Executors.newSingleThreadScheduledExecutor(new ConfigChangeMonitorThreadFactory(descriptor.getAppName()));

        watchTimer.scheduleWithFixedDelay(watcher, reloadIntervalMs, reloadIntervalMs, TimeUnit.MILLISECONDS);

        if (logger.isInfoEnabled())
        {
            logger.info("Reload interval: " + reloadIntervalMs);
        }
    }

    /**
     * Resolve a resource relative to an application root.
     * @param path the relative path to resolve
     * @return absolute path, may not actually exist (check with File.exists())
     */
    protected File toAbsoluteFile(String path)
    {
        final String muleHome = System.getProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY);
        String configPath = String.format("%s/apps/%s/%s", muleHome, getAppName(), path);
        return new File(configPath);
    }

    protected class ConfigFileWatcher extends AbstractFileWatcher
    {
        public ConfigFileWatcher(List<File> watchedResources)
        {
            super(watchedResources);
        }

        public void register() throws NotificationException
        {
            muleContext.registerListener(new FileWatcherNotificationListener(this));
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

    protected class FileWatcherNotificationListener implements MuleContextNotificationListener<MuleContextNotification>
    {
        private AbstractFileWatcher watcher;

        public FileWatcherNotificationListener(AbstractFileWatcher watcher)
        {
            this.watcher = watcher;
        }

        @Override
        public void onNotification(MuleContextNotification notification)
        {
            final int action = notification.getAction();
            switch (action)
            {
                case MuleContextNotification.CONTEXT_STARTED :
                    scheduleConfigMonitor(watcher);
                    break;

                case MuleContextNotification.CONTEXT_STOPPING :
                    if (watchTimer != null)
                    {
                        // edge case when app startup was interrupted and we haven't
                        // started monitoring it yet
                        watchTimer.shutdownNow();
                    }
                    muleContext.unregisterListener(this);
                    break;
            }
        }
    }

}
