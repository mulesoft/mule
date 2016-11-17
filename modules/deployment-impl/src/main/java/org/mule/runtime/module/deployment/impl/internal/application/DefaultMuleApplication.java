/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.SplashScreen.miniSplash;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.config.builders.SimpleConfigurationBuilder;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.util.ExceptionUtils;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.DeploymentStopException;
import org.mule.runtime.deployment.model.api.InstallException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.classloader.DisposableClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleApplication implements Application {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  protected final ApplicationDescriptor descriptor;
  private final DomainRepository domainRepository;
  private final List<ArtifactPlugin> artifactPlugins;
  private final ServiceRepository serviceRepository;
  private final ClassLoaderRepository classLoaderRepository;
  private final File location;
  private ApplicationStatus status;

  protected ArtifactClassLoader deploymentClassLoader;
  protected MuleContextListener muleContextListener;
  private ServerNotificationListener<MuleContextNotification> statusListener;
  private ArtifactContext artifactContext;

  public DefaultMuleApplication(ApplicationDescriptor descriptor,
                                MuleDeployableArtifactClassLoader deploymentClassLoader,
                                List<ArtifactPlugin> artifactPlugins, DomainRepository domainRepository,
                                ServiceRepository serviceRepository, File location,
                                ClassLoaderRepository classLoaderRepository) {
    this.descriptor = descriptor;
    this.domainRepository = domainRepository;
    this.serviceRepository = serviceRepository;
    this.classLoaderRepository = classLoaderRepository;
    this.artifactPlugins = artifactPlugins;
    this.location = location;
    this.deploymentClassLoader = deploymentClassLoader;
    updateStatusFor(NotInLifecyclePhase.PHASE_NAME);
    if (deploymentClassLoader == null) {
      throw new IllegalArgumentException("Classloader cannot be null");
    }
  }

  public void setMuleContextListener(MuleContextListener muleContextListener) {
    checkArgument(muleContextListener != null, "setMuleContextListener cannot be null");

    this.muleContextListener = muleContextListener;
  }

  @Override
  public void install() {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("New app '%s'", descriptor.getName())));
    }

    // set even though it might be redundant, just in case the app is been redeployed
    updateStatusFor(NotInLifecyclePhase.PHASE_NAME);

    try {
      for (String configResourceAbsolutePatch : this.descriptor.getAbsoluteResourcePaths()) {
        File configResource = new File(configResourceAbsolutePatch);
        if (!configResource.exists()) {
          String message = format("Config for app '%s' not found: %s", getArtifactName(), configResource);
          throw new InstallException(createStaticMessage(message));
        }
      }
    } catch (Exception e) {
      setStatusToFailed();
      throw e;
    }
  }

  @Override
  public ApplicationDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public Domain getDomain() {
    return domainRepository.getDomain(descriptor.getDomain());
  }

  @Override
  public void start() {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("Starting app '%s'", descriptor.getName())));
    }

    try {
      this.artifactContext.getMuleContext().start();

      // null CCL ensures we log at 'system' level
      // TODO getDomainClassLoader a more usable wrapper for any logger to be logged at sys level
      withContextClassLoader(null, () -> {
        ApplicationStartedSplashScreen splashScreen = new ApplicationStartedSplashScreen();
        splashScreen.createMessage(descriptor);
        logger.info(splashScreen.toString());
      });
    } catch (Exception e) {
      setStatusToFailed();

      // log it here so it ends up in app log, sys log will only log a message without stacktrace
      if (e instanceof MuleException) {
        logger.error(((MuleException) e).getDetailedMessage());
      } else {
        logger.error(null, ExceptionUtils.getRootCause(e));
      }

      throw new DeploymentStartException(createStaticMessage(format("Error starting application '%s'", descriptor.getName())), e);
    }
  }

  @Override
  public void init() {
    doInit(false);
  }

  private void doInit(boolean lazy) {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("Initializing app '%s'", descriptor.getName())));
    }

    try {
      ArtifactContextBuilder artifactBuilder =
          newBuilder().setArtifactProperties(descriptor.getAppProperties()).setArtifactType(APP)
              .setArtifactName(descriptor.getName()).setArtifactInstallationDirectory(descriptor.getArtifactLocation())
              .setConfigurationFiles(descriptor.getAbsoluteResourcePaths()).setDefaultEncoding(descriptor.getEncoding())
              .setArtifactPlugins(artifactPlugins).setExecutionClassloader(deploymentClassLoader.getClassLoader())
              .setEnableLazyInit(lazy).setServiceRepository(serviceRepository)
              .setClassLoaderRepository(classLoaderRepository);

      Domain domain = domainRepository.getDomain(descriptor.getDomain());
      if (domain.getMuleContext() != null) {
        artifactBuilder.setParentContext(domain.getMuleContext());
      }
      if (muleContextListener != null) {
        artifactBuilder.setMuleContextListener(muleContextListener);
      }
      artifactContext = artifactBuilder.build();
      setMuleContext(artifactContext.getMuleContext());
    } catch (Exception e) {
      setStatusToFailed();

      // log it here so it ends up in app log, sys log will only log a message without stacktrace
      logger.error(null, ExceptionUtils.getRootCause(e));
      throw new DeploymentInitException(createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
    }
  }

  @Override
  public void lazyInit() {
    doInit(true);
  }

  protected void setArtifactContext(final ArtifactContext artifactContext) throws NotificationException {
    this.artifactContext = artifactContext;
    setMuleContext(this.artifactContext.getMuleContext());
  }

  private void setMuleContext(final MuleContext muleContext) throws NotificationException {
    statusListener = new MuleContextNotificationListener<MuleContextNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(MuleContextNotification notification) {
        int action = notification.getAction();
        if (action == MuleContextNotification.CONTEXT_INITIALISED || action == MuleContextNotification.CONTEXT_STARTED
            || action == MuleContextNotification.CONTEXT_STOPPED || action == MuleContextNotification.CONTEXT_DISPOSED) {
          updateStatusFor(muleContext.getLifecycleManager().getCurrentPhase());
        }
      }
    };

    muleContext.registerListener(statusListener);
  }

  private void updateStatusFor(String phase) {
    status = ApplicationStatusMapper.getApplicationStatus(phase);
  }

  private void setStatusToFailed() {
    if (artifactContext != null) {
      artifactContext.getMuleContext().unregisterListener(statusListener);
    }

    status = ApplicationStatus.DEPLOYMENT_FAILED;
  }

  protected ConfigurationBuilder createConfigurationBuilderFromApplicationProperties() {
    // Load application properties first since they may be needed by other configuration builders
    final Map<String, String> appProperties = descriptor.getAppProperties();

    // Add the app.home variable to the context
    File appPath = new File(MuleContainerBootstrapUtils.getMuleAppsDir(), getArtifactName());
    appProperties.put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, appPath.getAbsolutePath());

    appProperties.put(MuleProperties.APP_NAME_PROPERTY, getArtifactName());

    return new SimpleConfigurationBuilder(appProperties);
  }

  @Override
  public MuleContext getMuleContext() {
    return artifactContext.getMuleContext();
  }

  @Override
  public File getLocation() {
    return location;
  }

  @Override
  public ConnectivityTestingService getConnectivityTestingService() {
    return artifactContext.getConnectivityTestingService();
  }

  @Override
  public MetadataService getMetadataService() {
    return artifactContext.getMetadataService();
  }

  @Override
  public void dispose() {
    // moved wrapper logic into the actual implementation, as redeploy() invokes it directly, bypassing
    // classloader cleanup
    try {
      ClassLoader appCl = null;
      if (getArtifactClassLoader() != null) {
        appCl = getArtifactClassLoader().getClassLoader();
      }
      // if not initialized yet, it can be null
      if (appCl != null) {
        Thread.currentThread().setContextClassLoader(appCl);
      }

      doDispose();

      if (appCl != null) {
        if (isRegionClassLoaderMember(appCl)) {
          ((DisposableClassLoader) appCl.getParent()).dispose();
        } else if (appCl instanceof DisposableClassLoader) {
          ((DisposableClassLoader) appCl).dispose();
        }
      }
    } finally {
      // kill any refs to the old classloader to avoid leaks
      Thread.currentThread().setContextClassLoader(null);
      deploymentClassLoader = null;
    }
  }

  private static boolean isRegionClassLoaderMember(ClassLoader classLoader) {
    return !(classLoader instanceof RegionClassLoader) && classLoader.getParent() instanceof RegionClassLoader;
  }

  @Override
  public String getArtifactName() {
    return descriptor.getName();
  }

  @Override
  public String getArtifactId() {
    return deploymentClassLoader.getArtifactId();
  }

  @Override
  public File[] getResourceFiles() {
    return descriptor.getConfigResourcesFile();
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return deploymentClassLoader;
  }

  @Override
  public void stop() {
    if (this.artifactContext == null
        || !this.artifactContext.getMuleContext().getLifecycleManager().isDirectTransition(Stoppable.PHASE_NAME)) {
      return;
    }

    if (this.artifactContext == null) {
      // app never started, maybe due to a previous error
      if (logger.isInfoEnabled()) {
        logger.info(format("Stopping app '%s' with no mule context", descriptor.getName()));
      }

      status = ApplicationStatus.STOPPED;
      return;
    }

    artifactContext.getMuleContext().getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);

    try {
      if (logger.isInfoEnabled()) {
        logger.info(miniSplash(format("Stopping app '%s'", descriptor.getName())));
      }

      this.artifactContext.getMuleContext().stop();
    } catch (MuleException e) {
      throw new DeploymentStopException(createStaticMessage(format("Error stopping application '%s'", descriptor.getName())), e);
    }
  }

  @Override
  public ApplicationStatus getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), descriptor.getName(), Integer.toHexString(System.identityHashCode(this)));
  }

  protected void doDispose() {
    if (artifactContext == null) {
      if (logger.isInfoEnabled()) {
        logger.info(format("App '%s' never started, nothing to dispose of", descriptor.getName()));
      }
      return;
    }

    if (artifactContext.getMuleContext().isStarted() && !artifactContext.getMuleContext().isDisposed()) {
      try {
        stop();
      } catch (DeploymentStopException e) {
        // catch the stop errors and just log, we're disposing of an app anyway
        logger.error("Error stopping application", e);
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("Disposing app '%s'", descriptor.getName())));
    }

    artifactContext.getMuleContext().dispose();
    artifactContext = null;
  }

}
