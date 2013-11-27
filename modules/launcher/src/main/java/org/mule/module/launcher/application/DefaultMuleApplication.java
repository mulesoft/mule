/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static org.mule.util.SplashScreen.miniSplash;
import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.DomainMuleContextAwareConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.builders.AutoConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.launcher.AbstractFileWatcher;
import org.mule.module.launcher.ConfigChangeMonitorThreadFactory;
import org.mule.module.launcher.DeploymentInitException;
import org.mule.module.launcher.DeploymentListener;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.DeploymentStopException;
import org.mule.module.launcher.InstallException;
import org.mule.module.launcher.MuleDeploymentService;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.artifact.MuleContextDeploymentListener;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.ClassUtils;
import org.mule.util.ExceptionUtils;

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
    protected transient final Log logger = LogFactory.getLog(getClass());
    protected transient final Log deployLogger = LogFactory.getLog(MuleDeploymentService.class);

    protected final ApplicationDescriptor descriptor;
    protected final ApplicationClassLoaderFactory applicationClassLoaderFactory;

    protected ScheduledExecutorService watchTimer;
    protected MuleContext muleContext;
    protected ArtifactClassLoader deploymentClassLoader;
    private Domain domain;

    protected String[] absoluteResourcePaths;

    protected DeploymentListener deploymentListener;
    private File[] configResourcesFile;

    protected DefaultMuleApplication(ApplicationDescriptor appDesc, ApplicationClassLoaderFactory applicationClassLoaderFactory)
    {
        this.descriptor = appDesc;
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.deploymentListener = new NullDeploymentListener();
        loadConfigResources();
    }

    public DefaultMuleApplication(ApplicationDescriptor descriptor, ApplicationClassLoaderFactory applicationClassLoaderFactory, Domain domain)
    {
        this(descriptor, applicationClassLoaderFactory);
        this.domain = domain;
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

        for (File configResource : configResourcesFile)
        {
            if (!configResource.exists())
            {
                String message = String.format("Config for app '%s' not found: %s", getArtifactName(), configResource);
                throw new InstallException(MessageFactory.createStaticMessage(message));
            }
        }
        deploymentClassLoader = applicationClassLoaderFactory.create(descriptor);
    }

    /**
     * @deprecated use getArtifactName instead.
     */
    @Deprecated
    @Override
    public String getAppName()
    {
        return getArtifactName();
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
                    muleContextFactory.addListener(new MuleContextDeploymentListener(getArtifactName(), deploymentListener));
                }
                ApplicationMuleContextBuilder applicationContextBuilder = new ApplicationMuleContextBuilder(descriptor);
                this.muleContext = muleContextFactory.createMuleContext(builders, applicationContextBuilder);
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
        if (domain == null || !domain.containsSharedResources())
        {
            return (ConfigurationBuilder) ClassUtils.instanciateClass(configBuilderClassName,
                                                                      new Object[] {absoluteResourcePaths}, getDeploymentClassLoader());
        }
        else
        {
            ConfigurationBuilder configurationBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(configBuilderClassName,
                                                                                                           new Object[] {absoluteResourcePaths}, getDeploymentClassLoader());
            if (configurationBuilder instanceof DomainMuleContextAwareConfigurationBuilder)
            {
                ((DomainMuleContextAwareConfigurationBuilder) configurationBuilder).setDomainContext(domain.getMuleContext());
            }
            else
            {
                //TODO verify if fail or throw exception
                throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("ConfigurationBuilder %s does not support domain context", configurationBuilder.getClass().getCanonicalName())));
            }
            return configurationBuilder;
        }
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
        File appPath = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), getArtifactName());
        appProperties.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, appPath.getAbsolutePath());

        appProperties.put(MuleProperties.APP_NAME_PROPERTY, getArtifactName());

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
        if (this.deploymentClassLoader == null)
        {
            return null;
        }
        return this.deploymentClassLoader.getClassLoader();
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

    /**
     * This method should be avoid.
     * <p/>
     * This logic belongs to DefaultMuleDeployer instead.
     * SEE MULE-7126.
     */
    @Override
    public void redeploy()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Redeploying app '%s'", descriptor.getAppName())));
        }

        String appName = getArtifactName();

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
    public String getArtifactName()
    {
        return descriptor.getAppName();
    }

    @Override
    public File[] getConfigResourcesFile()
    {
        return configResourcesFile;
    }

    @Override
    public ArtifactClassLoader getArtifactClassLoader()
    {
        return deploymentClassLoader;
    }

    @Override
    public void stop()
    {
        if (this.muleContext == null || !this.muleContext.getLifecycleManager().isDirectTransition(Stoppable.PHASE_NAME))
        {
            return;
        }

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
        String configPath = String.format("%s/apps/%s/%s", muleHome, getArtifactName(), path);
        return new File(configPath);
    }

    private void loadConfigResources()
    {
        // convert to absolute paths
        final String[] configResources = descriptor.getConfigResources();
        absoluteResourcePaths = new String[configResources.length];
        configResourcesFile = new File[absoluteResourcePaths.length];
        for (int i = 0; i < configResources.length; i++)
        {
            String resource = configResources[i];
            final File file = toAbsoluteFile(resource);
            configResourcesFile[i] = file;
            absoluteResourcePaths[i] = file.getAbsolutePath();
        }
    }

}
