/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
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
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;
import static org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager.isCompatibleBundle;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveDeploymentProperties;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.Notification.Action;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.logging.LogUtil;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.InstallException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifact;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.CompositeMuleContextDeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.domain.AmbiguousDomainReferenceException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.IncompatibleDomainException;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleApplication extends AbstractDeployableArtifact<ApplicationDescriptor> implements Application {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultMuleApplication.class);

  protected final ApplicationDescriptor descriptor;
  private final DomainRepository domainRepository;
  private final List<ArtifactPlugin> artifactPlugins;
  private final ServiceRepository serviceRepository;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;
  private final ClassLoaderRepository classLoaderRepository;
  private final File location;
  private ApplicationStatus status;

  protected MuleContextListener muleContextListener;
  private NotificationListener<MuleContextNotification> statusListener;
  private final ApplicationPolicyProvider policyManager;

  private NotificationListenerRegistry notificationRegistrer;

  private final LockFactory runtimeLockFactory;

  public DefaultMuleApplication(ApplicationDescriptor descriptor,
                                MuleDeployableArtifactClassLoader deploymentClassLoader,
                                List<ArtifactPlugin> artifactPlugins, DomainRepository domainRepository,
                                ServiceRepository serviceRepository,
                                ExtensionModelLoaderRepository extensionModelLoaderRepository, File location,
                                ClassLoaderRepository classLoaderRepository,
                                ApplicationPolicyProvider applicationPolicyProvider,
                                LockFactory runtimeLockFactory) {
    super("app", "application", deploymentClassLoader);
    this.descriptor = descriptor;
    this.domainRepository = domainRepository;
    this.serviceRepository = serviceRepository;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    this.classLoaderRepository = classLoaderRepository;
    this.artifactPlugins = artifactPlugins;
    this.location = location;
    this.policyManager = applicationPolicyProvider;
    this.runtimeLockFactory = runtimeLockFactory;
    updateStatusFor(NotInLifecyclePhase.PHASE_NAME);
    if (this.deploymentClassLoader == null) {
      throw new IllegalArgumentException("Classloader cannot be null");
    }
  }

  @Override
  public void setMuleContextListener(MuleContextListener muleContextListener) {
    checkArgument(muleContextListener != null, "setMuleContextListener cannot be null");

    if (this.muleContextListener != null) {
      CompositeMuleContextDeploymentListener compositeMuleContextDeploymentListener =
          new CompositeMuleContextDeploymentListener(this.muleContextListener);
      compositeMuleContextDeploymentListener.addDeploymentListener(muleContextListener);
      muleContextListener = compositeMuleContextDeploymentListener;
      ((DefaultMuleContext) artifactContext.getMuleContext()).removeListener(this.muleContextListener);
      ((DefaultMuleContext) artifactContext.getMuleContext()).addListener(muleContextListener);
    }

    this.muleContextListener = muleContextListener;
  }

  public MuleContextListener getMuleContextListener() {
    return muleContextListener;
  }

  @Override
  public void install() {
    withContextClassLoader(null, () -> {
      log(miniSplash(format("New %s '%s'", shortArtifactType, descriptor.getName())));
    });
    // set even though it might be redundant, just in case the app is been redeployed
    updateStatusFor(NotInLifecyclePhase.PHASE_NAME);

    try {
      for (String configFile : this.descriptor.getConfigResources()) {
        URL configFileUrl = getArtifactClassLoader().getClassLoader().getResource(configFile);
        if (configFileUrl == null) {
          String message = format("Config for %s '%s' not found: %s", shortArtifactType, getArtifactName(), configFile);
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
    try {
      return getApplicationDomain(domainRepository, descriptor);
    } catch (DomainNotFoundException | IncompatibleDomainException | AmbiguousDomainReferenceException e) {
      return null;
    }
  }

  @Override
  public void start() {
    withContextClassLoader(null, () -> {
      log(miniSplash(format("Starting %s '%s'", shortArtifactType, descriptor.getName())));
    });
    try {
      this.artifactContext.getMuleContext().start();

      // null CCL ensures we log at 'system' level
      // TODO getDomainClassLoader a more usable wrapper for any logger to be logged at sys level
      withContextClassLoader(null, () -> {
        ApplicationStartedSplashScreen splashScreen = new ApplicationStartedSplashScreen();
        splashScreen.createMessage(descriptor);
        LogUtil.log(splashScreen.toString());
      });
    } catch (Exception e) {
      setStatusToFailed();

      // log it here so it ends up in app log, sys log will only log a message without stacktrace
      if (e instanceof MuleException) {
        LOGGER.error(((MuleException) e).getDetailedMessage());
      } else {
        LOGGER.error(null, getRootCause(e));
      }

      throw new DeploymentStartException(createStaticMessage(format("Error starting %s '%s'", artifactType,
                                                                    descriptor.getName())),
                                         e);
    }
  }

  @Override
  public void init() {
    doInit(false, false);
  }

  private void doInit(boolean lazy, boolean disableXmlValidations) {
    withContextClassLoader(null, () -> {
      log(miniSplash(format("Initializing %s '%s'", shortArtifactType, descriptor.getName())));
    });
    try {
      ArtifactContextBuilder artifactBuilder =
          newBuilder().setArtifactProperties(merge(descriptor.getAppProperties(), getProperties())).setArtifactType(APP)
              .setDataFolderName(descriptor.getDataFolderName())
              .setArtifactName(descriptor.getName()).setArtifactInstallationDirectory(descriptor.getArtifactLocation())
              .setConfigurationFiles(descriptor.getConfigResources().toArray(new String[descriptor.getConfigResources().size()]))
              .setDefaultEncoding(descriptor.getEncoding())
              .setArtifactPlugins(artifactPlugins).setExecutionClassloader(deploymentClassLoader.getClassLoader())
              .setEnableLazyInit(lazy).setDisableXmlValidations(disableXmlValidations).setServiceRepository(serviceRepository)
              .setExtensionModelLoaderRepository(extensionModelLoaderRepository)
              .setClassLoaderRepository(classLoaderRepository)
              .setArtifactDeclaration(descriptor.getArtifactDeclaration())
              .setProperties(ofNullable(resolveDeploymentProperties(descriptor.getDataFolderName(),
                                                                    descriptor.getDeploymentProperties())))
              .setPolicyProvider(policyManager)
              .setRuntimeLockFactory(runtimeLockFactory);

      Domain domain = getApplicationDomain(domainRepository, descriptor);
      if (domain.getRegistry() != null) {
        artifactBuilder.setParentArtifact(domain);
      }
      if (muleContextListener != null) {
        artifactBuilder.setMuleContextListener(muleContextListener);
      }
      artifactContext = artifactBuilder.build();
      setMuleContext(artifactContext.getMuleContext(), artifactContext.getRegistry());
    } catch (Exception e) {
      setStatusToFailed();

      // log it here so it ends up in app log, sys log will only log a message without stacktrace
      LOGGER.error(e.getMessage(), getRootCause(e));
      throw new DeploymentInitException(createStaticMessage(getRootCauseMessage(e)), e);
    }
  }

  private Properties getProperties() {
    if (!descriptor.getDeploymentProperties().isPresent()) {
      return new Properties();
    }

    return descriptor.getDeploymentProperties().get();
  }

  private Map<String, String> merge(Map<String, String> properties, Properties deploymentProperties) {
    if (deploymentProperties == null) {
      return properties;
    }

    Map<String, String> mergedProperties = new HashMap<>();
    mergedProperties.putAll(properties);
    for (Map.Entry<Object, Object> entry : deploymentProperties.entrySet()) {
      mergedProperties.put(entry.getKey().toString(), entry.getValue().toString());
    }

    return mergedProperties;
  }

  @Override
  public void lazyInit() {
    doInit(true, true);
  }

  @Override
  public void lazyInit(boolean disableXmlValidations) {
    doInit(true, disableXmlValidations);
  }

  protected void setArtifactContext(final ArtifactContext artifactContext) {
    this.artifactContext = artifactContext;
    setMuleContext(artifactContext.getMuleContext(), artifactContext.getRegistry());
  }

  private void setMuleContext(final MuleContext muleContext, Registry registry) {
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

    notificationRegistrer = registry.lookupByType(NotificationListenerRegistry.class).get();
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
  public Registry getRegistry() {
    return artifactContext != null ? artifactContext.getRegistry() : null;
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
  public String getArtifactName() {
    return descriptor.getName();
  }

  @Override
  public String getArtifactId() {
    return deploymentClassLoader.getArtifactId();
  }

  @Override
  public File[] getResourceFiles() {
    return descriptor.getConfigResources().stream()
        .map(configFile -> new File(getLocation(), configFile))
        .collect(Collectors.toList())
        .toArray(new File[descriptor.getConfigResources().size()]);
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return deploymentClassLoader;
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

  protected ArtifactClassLoader getDeploymentClassLoader() {
    return deploymentClassLoader;
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), descriptor.getName(), Integer.toHexString(System.identityHashCode(this)));
  }

  static Domain getApplicationDomain(DomainRepository domainRepository, ApplicationDescriptor descriptor)
      throws DomainNotFoundException, IncompatibleDomainException, AmbiguousDomainReferenceException {
    Domain resolvedDomain = resolveApplicationDomain(domainRepository, descriptor);
    descriptor.setDomainName(resolvedDomain.getDescriptor().getName());
    return resolvedDomain;
  }

  private static Domain resolveApplicationDomain(DomainRepository domainRepository, ApplicationDescriptor descriptor)
      throws DomainNotFoundException, IncompatibleDomainException, AmbiguousDomainReferenceException {
    String configuredDomainName = descriptor.getDomainName();
    Optional<BundleDescriptor> domainBundleDescriptor = descriptor.getDomainDescriptor();

    boolean shouldUseDefaultDomain = (configuredDomainName == null) || DEFAULT_DOMAIN_NAME.equals(configuredDomainName);
    if (!shouldUseDefaultDomain && !domainBundleDescriptor.isPresent()) {
      throw new IllegalStateException(format("Dependency for domain '%s' was not declared", configuredDomainName));
    }

    if (!domainBundleDescriptor.isPresent()) {
      return domainRepository.getDomain(DEFAULT_DOMAIN_NAME);
    }

    if (configuredDomainName != null) {
      Domain foundDomain = domainRepository.getDomain(configuredDomainName);
      if (isCompatibleBundle(foundDomain.getDescriptor().getBundleDescriptor(), domainBundleDescriptor.get())) {
        return foundDomain;
      } else {
        throw new IncompatibleDomainException(configuredDomainName, foundDomain);
      }
    }

    return domainRepository.getCompatibleDomain(domainBundleDescriptor.get());
  }

}
