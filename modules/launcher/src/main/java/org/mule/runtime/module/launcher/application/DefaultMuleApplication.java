/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static java.lang.String.format;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.SplashScreen.miniSplash;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.DisposableClassLoader;
import org.mule.runtime.module.launcher.DeploymentInitException;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.DeploymentStartException;
import org.mule.runtime.module.launcher.DeploymentStopException;
import org.mule.runtime.module.launcher.InstallException;
import org.mule.runtime.module.launcher.MuleDeploymentService;
import org.mule.runtime.module.launcher.artifact.ArtifactMuleContextBuilder;
import org.mule.runtime.module.launcher.artifact.MuleContextDeploymentListener;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.domain.Domain;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleApplication implements Application
{

    protected transient final Logger logger = LoggerFactory.getLogger(getClass());
    protected transient final Logger deployLogger = LoggerFactory.getLogger(MuleDeploymentService.class);

    protected final ApplicationDescriptor descriptor;
    private final List<ApplicationPlugin> applicationPlugins;
    private final DomainRepository domainRepository;
    private ApplicationStatus status;

    protected MuleContext muleContext;
    protected ArtifactClassLoader deploymentClassLoader;
    protected DeploymentListener deploymentListener;
    private ServerNotificationListener<MuleContextNotification> statusListener;

    public DefaultMuleApplication(ApplicationDescriptor descriptor, ArtifactClassLoader deploymentClassLoader, List<ApplicationPlugin> applicationPlugins, DomainRepository domainRepository)
    {
        this.descriptor = descriptor;
        this.applicationPlugins = applicationPlugins;
        this.domainRepository = domainRepository;
        this.deploymentListener = new NullDeploymentListener();
        updateStatusFor(NotInLifecyclePhase.PHASE_NAME);
        if (deploymentClassLoader == null)
        {
            throw new IllegalArgumentException("Classloader cannot be null");
        }
        this.deploymentClassLoader = deploymentClassLoader;
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
            logger.info(miniSplash(format("New app '%s'", descriptor.getName())));
        }

        // set even though it might be redundant, just in case the app is been redeployed
        updateStatusFor(NotInLifecyclePhase.PHASE_NAME);

        for (String configResourceAbsolutePatch : this.descriptor.getAbsoluteResourcePaths())
        {
            File configResource = new File(configResourceAbsolutePatch);
            if (!configResource.exists())
            {
                String message = format("Config for app '%s' not found: %s", getArtifactName(), configResource);
                throw new InstallException(createStaticMessage(message));
            }
        }
    }

    @Override
    public ApplicationDescriptor getDescriptor()
    {
        return descriptor;
    }

    @Override
    public Domain getDomain()
    {
        return domainRepository.getDomain(descriptor.getDomain());
    }

    public void setAppName(String appName)
    {
        this.descriptor.setName(appName);
    }

    @Override
    public void start()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(format("Starting app '%s'", descriptor.getName())));
        }

        try
        {
            this.muleContext.start();

            // null CCL ensures we log at 'system' level
            // TODO getDomainClassLoader a more usable wrapper for any logger to be logged at sys level
            withContextClassLoader(null, () -> {
                ApplicationStartedSplashScreen splashScreen = new ApplicationStartedSplashScreen();
                splashScreen.createMessage(descriptor);
                deployLogger.info(splashScreen.toString());
            });
        }
        catch (Exception e)
        {
            setStatusToFailed();

            // log it here so it ends up in app log, sys log will only log a message without stacktrace
            if (e instanceof MuleException)
            {
                logger.error(((MuleException) e).getDetailedMessage());
            }
            else
            {
                logger.error(null, ExceptionUtils.getRootCause(e));
            }

            throw new DeploymentStartException(createStaticMessage(format("Error starting application '%s'", descriptor.getName())), e);
        }
    }

    @Override
    public void init()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(format("Initializing app '%s'", descriptor.getName())));
        }

        try
        {
            ArtifactMuleContextBuilder artifactBuilder = new ArtifactMuleContextBuilder()
                    .setArtifactProperties(descriptor.getAppProperties())
                    .setArtifactType(APP)
                    .setArtifactInstallationDirectory(new File(MuleContainerBootstrapUtils.getMuleAppsDir(), getArtifactName()))
                    .setConfigurationFiles(descriptor.getAbsoluteResourcePaths())
                    .setDefaultEncoding(descriptor.getEncoding())
                    .setApplicationPlugins(applicationPlugins)
                    .setExecutionClassloader(deploymentClassLoader.getClassLoader());

            Domain domain = domainRepository.getDomain(descriptor.getDomain());
            if (domain.getMuleContext() != null)
            {
                artifactBuilder.setParentContext(domain.getMuleContext());
            }
            if (deploymentListener != null)
            {
                artifactBuilder.setMuleContextListener(new MuleContextDeploymentListener(getArtifactName(), deploymentListener));
            }
            setMuleContext(artifactBuilder.build());
        }
        catch (Exception e)
        {
            setStatusToFailed();

            // log it here so it ends up in app log, sys log will only log a message without stacktrace
            logger.error(null, ExceptionUtils.getRootCause(e));
            throw new DeploymentInitException(createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
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
        return descriptor.getName();
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
                logger.info(format("Stopping app '%s' with no mule context", descriptor.getName()));
            }

            status = ApplicationStatus.STOPPED;
            return;
        }

        muleContext.getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);

        try
        {
            if (logger.isInfoEnabled())
            {
                logger.info(miniSplash(format("Stopping app '%s'", descriptor.getName())));
            }

            this.muleContext.stop();
        }
        catch (MuleException e)
        {
            throw new DeploymentStopException(createStaticMessage(format("Error stopping application '%s'", descriptor.getName())), e);
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
        return format("%s[%s]@%s", getClass().getName(),
                      descriptor.getName(),
                      Integer.toHexString(System.identityHashCode(this)));
    }

    protected void doDispose()
    {
        if (muleContext == null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info(format("App '%s' never started, nothing to dispose of", descriptor.getName()));
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
                logger.error("Error stopping application", e);
            }
        }
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(format("Disposing app '%s'", descriptor.getName())));
        }

        muleContext.dispose();
        muleContext = null;
    }

}
