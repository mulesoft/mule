/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_DISPOSED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_INITIALISED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTED;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STOPPED;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;

import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.IntegerAction;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.NotificationListener;
import org.mule.runtime.core.api.context.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.context.notification.Notification.Action;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
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
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.DisposableClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleApplication implements Application {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  protected final ApplicationDescriptor descriptor;
  private final DomainRepository domainRepository;
  private final List<ArtifactPlugin> artifactPlugins;
  private final ServiceRepository serviceRepository;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;
  private final ClassLoaderRepository classLoaderRepository;
  private final File location;
  private ApplicationStatus status;

  protected ArtifactClassLoader deploymentClassLoader;
  protected MuleContextListener muleContextListener;
  private NotificationListener<MuleContextNotification> statusListener;
  private ArtifactContext artifactContext;
  private ApplicationPolicyProvider policyManager;

  private NotificationListenerRegistry notificationRegistrer;

  public DefaultMuleApplication(ApplicationDescriptor descriptor,
                                MuleDeployableArtifactClassLoader deploymentClassLoader,
                                List<ArtifactPlugin> artifactPlugins, DomainRepository domainRepository,
                                ServiceRepository serviceRepository,
                                ExtensionModelLoaderRepository extensionModelLoaderRepository, File location,
                                ClassLoaderRepository classLoaderRepository,
                                ApplicationPolicyProvider applicationPolicyProvider) {
    this.descriptor = descriptor;
    this.domainRepository = domainRepository;
    this.serviceRepository = serviceRepository;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    this.classLoaderRepository = classLoaderRepository;
    this.artifactPlugins = artifactPlugins;
    this.location = location;
    this.deploymentClassLoader = deploymentClassLoader;
    this.policyManager = applicationPolicyProvider;
    updateStatusFor(NotInLifecyclePhase.PHASE_NAME);
    if (deploymentClassLoader == null) {
      throw new IllegalArgumentException("Classloader cannot be null");
    }
  }

  @Override
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
      for (String configResourceAbsolutePath : this.descriptor.getAbsoluteResourcePaths()) {
        File configResource = new File(configResourceAbsolutePath);
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
        logger.error(null, getRootCause(e));
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
              .setExtensionModelLoaderRepository(extensionModelLoaderRepository)
              .setClassLoaderRepository(classLoaderRepository)
              .setArtifactDeclaration(descriptor.getArtifactDeclaration())
              .setPolicyProvider(policyManager);

      Domain domain = domainRepository.getDomain(descriptor.getDomain());
      if (domain.getMuleContext() != null) {
        artifactBuilder.serParenArtifact(domain);
      }
      if (muleContextListener != null) {
        artifactBuilder.setMuleContextListener(muleContextListener);
      }
      artifactContext = artifactBuilder.build();
      setMuleContext(artifactContext.getMuleContext());
    } catch (Exception e) {
      setStatusToFailed();

      // log it here so it ends up in app log, sys log will only log a message without stacktrace
      logger.error(null, getRootCause(e));
      throw new DeploymentInitException(createStaticMessage(getRootCauseMessage(e)), e);
    }
  }

  @Override
  public void lazyInit() {
    doInit(true);
  }

  protected void setArtifactContext(final ArtifactContext artifactContext) {
    this.artifactContext = artifactContext;
    setMuleContext(this.artifactContext.getMuleContext());
  }

  private void setMuleContext(final MuleContext muleContext) {
    statusListener = new MuleContextNotificationListener<MuleContextNotification>() {

      @Override
      public boolean isBlocking() {
        return false;
      }

      @Override
      public void onNotification(MuleContextNotification notification) {
        Action action = notification.getAction();
        if (new IntegerAction(CONTEXT_INITIALISED).equals(action) || new IntegerAction(CONTEXT_STARTED).equals(action)
            || new IntegerAction(CONTEXT_STOPPED).equals(action) || new IntegerAction(CONTEXT_DISPOSED).equals(action)) {
          updateStatusFor(muleContext.getLifecycleManager().getCurrentPhase());
        }
      }
    };

    try {
      notificationRegistrer = muleContext.getRegistry().lookupObject(NotificationListenerRegistry.class);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
    notificationRegistrer.registerListener(statusListener);
  }

  private void updateStatusFor(String phase) {
    status = ApplicationStatusMapper.getApplicationStatus(phase);
  }

  private void setStatusToFailed() {
    if (artifactContext != null) {
      notificationRegistrer.unregisterListener(statusListener);
    }

    status = ApplicationStatus.DEPLOYMENT_FAILED;
  }

  @Override
  public MuleContext getMuleContext() {
    return artifactContext != null ? artifactContext.getMuleContext() : null;
  }

  @Override
  public File getLocation() {
    return location;
  }

  @Override
  public ConnectivityTestingService getConnectivityTestingService() {
    return (ConnectivityTestingService) artifactContext.getRegistry().lookupByName(CONNECTIVITY_TESTING_SERVICE_KEY).get();
  }

  @Override
  public MetadataService getMetadataService() {
    return (MetadataService) artifactContext.getRegistry().lookupByName(METADATA_SERVICE_KEY).get();
  }

  @Override
  public ValueProviderService getValueProviderService() {
    return (ValueProviderService) artifactContext.getRegistry().lookupByName(VALUE_PROVIDER_SERVICE_KEY).get();
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
  public RegionClassLoader getRegionClassLoader() {
    ClassLoader parentClassLoader = deploymentClassLoader.getClassLoader().getParent();

    if (parentClassLoader instanceof RegionClassLoader) {
      return (RegionClassLoader) parentClassLoader;
    } else {
      throw new IllegalStateException("Application is not a region owner");
    }
  }

  @Override
  public ApplicationPolicyProvider getPolicyManager() {
    return policyManager;
  }

  @Override
  public List<ArtifactPlugin> getArtifactPlugins() {
    return artifactPlugins;
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

    try {
      stop();
    } catch (DeploymentStopException e) {
      // catch the stop errors and just log, we're disposing of an app anyway
      logger.error("Error stopping application", e);
    }

    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("Disposing app '%s'", descriptor.getName())));
    }

    artifactContext.getMuleContext().dispose();
    artifactContext = null;
  }

}
