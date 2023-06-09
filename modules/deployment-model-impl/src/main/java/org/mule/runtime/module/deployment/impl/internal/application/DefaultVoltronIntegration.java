/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.Notification.Action;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.logging.LogUtil;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.InstallException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifact;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.VoltronArtifactContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.AmbiguousDomainReferenceException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.IncompatibleDomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

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
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.*;
import static org.mule.runtime.core.api.data.sample.SampleDataService.SAMPLE_DATA_SERVICE_KEY;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;
import static org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager.isCompatibleBundle;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveDeploymentProperties;

public class DefaultVoltronIntegration extends AbstractDeployableArtifact<ApplicationDescriptor> implements Application {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultVoltronIntegration.class);

  protected final ApplicationDescriptor descriptor;
  private final DomainRepository domainRepository;
  private final List<ArtifactPlugin> artifactPlugins;
  private final ServiceRepository serviceRepository;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;
  private final ClassLoaderRepository classLoaderRepository;
  private final File location;
  private final MemoryManagementService memoryManagementService;
  private final ExpressionLanguageMetadataService expressionLanguageMetadataService;
  private final ArtifactConfigurationProcessor artifactConfigurationProcessor;
  private final ArtifactAst artifactAst;
  private ApplicationStatus status;

  protected MuleContextListener muleContextListener;
  private NotificationListener<MuleContextNotification> statusListener;
  private final ApplicationPolicyProvider policyManager;

  private NotificationListenerRegistry notificationRegistrer;

  private final LockFactory runtimeLockFactory;

  public DefaultVoltronIntegration(ApplicationDescriptor descriptor,
                                   MuleDeployableArtifactClassLoader deploymentClassLoader,
                                   List<ArtifactPlugin> artifactPlugins, DomainRepository domainRepository,
                                   ServiceRepository serviceRepository,
                                   ExtensionModelLoaderRepository extensionModelLoaderRepository, File location,
                                   ClassLoaderRepository classLoaderRepository,
                                   ApplicationPolicyProvider applicationPolicyProvider,
                                   LockFactory runtimeLockFactory,
                                   MemoryManagementService memoryManagementService,
                                   ArtifactConfigurationProcessor artifactConfigurationProcessor, ArtifactAst artifactAst) {
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
    this.memoryManagementService = memoryManagementService;
    this.expressionLanguageMetadataService = getExpressionLanguageMetadataService(serviceRepository);
    this.artifactConfigurationProcessor = artifactConfigurationProcessor;
    this.artifactAst = artifactAst;
    updateStatusFor(NotInLifecyclePhase.PHASE_NAME);
    if (this.deploymentClassLoader == null) {
      throw new IllegalArgumentException("Classloader cannot be null");
    }
  }

  private static ExpressionLanguageMetadataService getExpressionLanguageMetadataService(ServiceRepository serviceRepository) {
    if (serviceRepository == null) {
      return null;
    }
    return serviceRepository.getServices().stream()
        .filter(ExpressionLanguageMetadataService.class::isInstance)
        .findFirst()
        .map(ExpressionLanguageMetadataService.class::cast)
        .orElse(null);
  }

  @Override
  public void setMuleContextListener(MuleContextListener muleContextListener) {
    checkArgument(muleContextListener != null, "setMuleContextListener cannot be null");

    this.muleContextListener = muleContextListener;
  }

  @Override
  public void install() {
    log(format("New %s '%s'", shortArtifactType, descriptor.getName()));
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
    log(format("Starting %s '%s'", shortArtifactType, descriptor.getName()));
    try {
      this.artifactContext.getMuleContext().start();
      // persistArtifactState(START);

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
    doInit(false, false, false);
  }

  @Override
  public void initTooling() {
    doInit(false, false, true);
  }

  private void doInit(boolean lazy, boolean disableXmlValidations, boolean addToolingObjectsToRegistry) {
    withContextClassLoader(null, () -> {
      log(miniSplash(format("Initializing %s '%s'", shortArtifactType, descriptor.getName())));
    });
    try {
      VoltronArtifactContextBuilder artifactBuilder =
          VoltronArtifactContextBuilder
              .newBuilder()
              .setArtifactProperties(merge(descriptor.getAppProperties(), getProperties()))
              .setArtifactConfigurationProcessor(null)
              .setDataFolderName(descriptor.getDataFolderName())
              .setArtifactName(descriptor.getName())
              .setArtifactConfigurationProcessor(artifactConfigurationProcessor)
              .setDefaultEncoding(descriptor.getEncoding())
              .setExecutionClassloader(deploymentClassLoader.getClassLoader())
              .setServiceRepository(serviceRepository)
              .setExtensionModelLoaderRepository(extensionModelLoaderRepository)
              .setClassLoaderRepository(classLoaderRepository)
              .setProperties(ofNullable(resolveDeploymentProperties(descriptor.getDataFolderName(),
                                                                    descriptor.getDeploymentProperties())))
              .setRuntimeLockFactory(runtimeLockFactory)
              .setMemoryManagementService(memoryManagementService)
              .setExpressionLanguageMetadataService(expressionLanguageMetadataService);

      Domain domain = getApplicationDomain(domainRepository, descriptor);
      if (domain.getArtifactContext() != null) {
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
    doInit(true, true, false);
  }

  @Override
  public void lazyInit(boolean disableXmlValidations) {
    doInit(true, disableXmlValidations, false);
  }

  @Override
  public void lazyInitTooling(boolean disableXmlValidations) {
    doInit(true, disableXmlValidations, true);
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
  public SampleDataService getSampleDataService() {
    return (SampleDataService) artifactContext.getRegistry().lookupByName(SAMPLE_DATA_SERVICE_KEY).get();
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
