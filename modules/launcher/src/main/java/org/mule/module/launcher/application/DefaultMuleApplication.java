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
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.lifecycle.phases.NotInLifecyclePhase;
import org.mule.module.launcher.DeploymentInitException;
import org.mule.module.launcher.DeploymentListener;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.config.builders.ExtensionsManagerConfigurationBuilder;
import org.mule.module.launcher.DeploymentStopException;
import org.mule.module.launcher.DisposableClassLoader;
import org.mule.module.launcher.InstallException;
import org.mule.module.launcher.MuleDeploymentService;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.artifact.MuleContextDeploymentListener;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.ClassUtils;
import org.mule.util.ExceptionUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleApplication implements Application
{

    protected transient final Log logger = LogFactory.getLog(getClass());
    protected transient final Log deployLogger = LogFactory.getLog(MuleDeploymentService.class);

    protected final ApplicationDescriptor descriptor;
    protected final ApplicationClassLoaderFactory applicationClassLoaderFactory;
    private ApplicationStatus status;

    protected MuleContext muleContext;
    protected ArtifactClassLoader deploymentClassLoader;
    private Domain domain;
    protected DeploymentListener deploymentListener;
    private ServerNotificationListener<MuleContextNotification> statusListener;

    public DefaultMuleApplication(ApplicationDescriptor descriptor, ApplicationClassLoaderFactory applicationClassLoaderFactory, Domain domain)
    {
        this.descriptor = descriptor;
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.deploymentListener = new NullDeploymentListener();
        this.domain = domain;
        updateStatusFor(NotInLifecyclePhase.PHASE_NAME);
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

        // set even though it might be redundant, just in case the app is been redeployed
        updateStatusFor(NotInLifecyclePhase.PHASE_NAME);

        for (String configResourceAbsolutePatch : this.descriptor.getAbsoluteResourcePaths())
        {
            File configResource = new File(configResourceAbsolutePatch);
            if (!configResource.exists())
            {
                String message = String.format("Config for app '%s' not found: %s", getArtifactName(), configResource);
                throw new InstallException(MessageFactory.createStaticMessage(message));
            }
        }
        deploymentClassLoader = applicationClassLoaderFactory.create(descriptor);
    }

    @Override
    public ApplicationDescriptor getDescriptor()
    {
        return descriptor;
    }

    @Override
    public Domain getDomain()
    {
        return domain;
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
            // TODO getDomainClassLoader a more usable wrapper for any logger to be logged at sys level
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
        catch (Exception e)
        {
            setStatusToFailed();

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
            ConfigurationBuilder cfgBuilder = domain.createApplicationConfigurationBuilder(this);
            if (!cfgBuilder.isConfigured())
            {
                List<ConfigurationBuilder> builders = new LinkedList<>();
                builders.add(createConfigurationBuilderFromApplicationProperties());
                builders.add(createApplicationLevelExtensionManagerBuilder());

                // We need to add this builder before spring so that we can use Mule annotations in Spring or any other builder
                addAnnotationsConfigBuilderIfPresent(builders);

                builders.add(cfgBuilder);

                DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
                if (deploymentListener != null)
                {
                    muleContextFactory.addListener(new MuleContextDeploymentListener(getArtifactName(), deploymentListener));
                }

                ApplicationMuleContextBuilder applicationContextBuilder = new ApplicationMuleContextBuilder(descriptor);
                setMuleContext(muleContextFactory.createMuleContext(builders, applicationContextBuilder));
            }
        }
        catch (Exception e)
        {
            setStatusToFailed();

            // log it here so it ends up in app log, sys log will only log a message without stacktrace
            logger.error(null, ExceptionUtils.getRootCause(e));
            throw new DeploymentInitException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
        }
    }

    protected void setMuleContext(final MuleContext muleContext) throws NotificationException
    {
        statusListener = new MuleContextNotificationListener<MuleContextNotification>()
        {
            @Override
            public void onNotification(MuleContextNotification notification)
            {
                int action = notification.getAction();
                if (action == MuleContextNotification.CONTEXT_INITIALISED ||
                    action == MuleContextNotification.CONTEXT_STARTED ||
                    action == MuleContextNotification.CONTEXT_STOPPED ||
                    action == MuleContextNotification.CONTEXT_DISPOSED)
                {
                    updateStatusFor(muleContext.getLifecycleManager().getCurrentPhase());
                }
            }
        };

        muleContext.registerListener(statusListener);
        this.muleContext = muleContext;
    }

    private void updateStatusFor(String phase)
    {
        status = ApplicationStatusMapper.getApplicationStatus(phase);
    }

    private void setStatusToFailed()
    {
        if (muleContext != null)
        {
            muleContext.unregisterListener(statusListener);
        }

        status = ApplicationStatus.DEPLOYMENT_FAILED;
    }

    protected ConfigurationBuilder createConfigurationBuilderFromApplicationProperties()
    {
        // Load application properties first since they may be needed by other configuration builders
        final Map<String, String> appProperties = descriptor.getAppProperties();

        // Add the app.home variable to the context
        File appPath = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), getArtifactName());
        appProperties.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, appPath.getAbsolutePath());

        appProperties.put(MuleProperties.APP_NAME_PROPERTY, getArtifactName());

        return new SimpleConfigurationBuilder(appProperties);
    }

    private ConfigurationBuilder createApplicationLevelExtensionManagerBuilder()
    {
        return new ExtensionsManagerConfigurationBuilder();
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
    public void dispose()
    {
        // moved wrapper logic into the actual implementation, as redeploy() invokes it directly, bypassing
        // classloader cleanup
        try
        {
            ClassLoader appCl = null;
            if (getArtifactClassLoader() != null)
            {
                appCl = getArtifactClassLoader().getClassLoader();
            }
            // if not initialized yet, it can be null
            if (appCl != null)
            {
                Thread.currentThread().setContextClassLoader(appCl);
            }

            doDispose();

            if (appCl != null)
            {
                // close classloader to release jar connections in lieu of Java 7's ClassLoader.close()
                if (appCl instanceof DisposableClassLoader)
                {
                    ((DisposableClassLoader) appCl).dispose();
                }
            }
        }
        finally
        {
            // kill any refs to the old classloader to avoid leaks
            Thread.currentThread().setContextClassLoader(null);
            deploymentClassLoader = null;
        }
    }

    @Override
    public String getArtifactName()
    {
        return descriptor.getAppName();
    }

    @Override
    public File[] getResourceFiles()
    {
        return descriptor.getConfigResourcesFile();
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

            status = ApplicationStatus.STOPPED;
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
    public ApplicationStatus getStatus()
    {
        return status;
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

}
