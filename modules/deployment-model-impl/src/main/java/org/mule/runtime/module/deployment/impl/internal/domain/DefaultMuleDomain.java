/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveDeploymentProperties;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.InstallException;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableArtifact;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleDomain extends AbstractDeployableArtifact<DomainDescriptor> implements Domain {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultMuleDomain.class);

  private final DomainDescriptor descriptor;
  private final ServiceRepository serviceRepository;
  private final List<ArtifactPlugin> artifactPlugins;
  private final ExtensionModelLoaderManager extensionModelLoaderManager;
  private final ClassLoaderRepository classLoaderRepository;
  private final ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider;
  private final LockFactory runtimeLockFactory;

  private MuleContextListener muleContextListener;

  public DefaultMuleDomain(DomainDescriptor descriptor, ArtifactClassLoader deploymentClassLoader,
                           ClassLoaderRepository classLoaderRepository,
                           ServiceRepository serviceRepository,
                           List<ArtifactPlugin> artifactPlugins,
                           ExtensionModelLoaderManager extensionModelLoaderManager,
                           ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider,
                           LockFactory runtimeLockFactory) {
    super("domain", "domain", deploymentClassLoader);
    this.classLoaderRepository = classLoaderRepository;
    this.descriptor = descriptor;
    this.serviceRepository = serviceRepository;
    this.artifactPlugins = artifactPlugins;
    this.extensionModelLoaderManager = extensionModelLoaderManager;
    this.runtimeComponentBuildingDefinitionProvider = runtimeComponentBuildingDefinitionProvider;
    this.runtimeLockFactory = runtimeLockFactory;
  }

  @Override
  public void setMuleContextListener(MuleContextListener muleContextListener) {
    checkArgument(muleContextListener != null, "muleContextListener cannot be null");

    this.muleContextListener = muleContextListener;
  }

  public String getName() {
    return descriptor.getName();
  }

  @Override
  public Registry getRegistry() {
    return artifactContext != null ? artifactContext.getRegistry() : null;
  }

  @Override
  public File getLocation() {
    return descriptor.getArtifactLocation();
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
  public List<ArtifactPlugin> getArtifactPlugins() {
    return artifactPlugins;
  }

  @Override
  public void install() {
    Thread currentThread = currentThread();
    final ClassLoader originalClassLoader = currentThread().getContextClassLoader();
    currentThread.setContextClassLoader(null);
    try {
      if (LOGGER.isInfoEnabled()) {
        log(miniSplash(format("New domain '%s'", getArtifactName())));
      }
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }

    try {
      for (String configFile : this.descriptor.getConfigResources()) {
        URL configFileUrl = getArtifactClassLoader().getClassLoader().getResource(configFile);
        if (configFileUrl == null) {
          String message = format("Config for domain '%s' not found: %s", getArtifactName(), configFile);
          throw new InstallException(createStaticMessage(message));
        }
      }
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public void init() {
    doInit(false, false);
  }

  @Override
  public void lazyInit() {
    doInit(true, true);
  }

  @Override
  public void lazyInit(boolean disableXmlValidations) {
    doInit(true, disableXmlValidations);
  }

  public void doInit(boolean lazy, boolean disableXmlValidations) throws DeploymentInitException {
    Thread currentThread = currentThread();
    final ClassLoader originalClassLoader = currentThread().getContextClassLoader();
    currentThread.setContextClassLoader(null);
    try {
      if (LOGGER.isInfoEnabled()) {
        log(miniSplash(format("Initializing domain '%s'", getArtifactName())));
      }
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }

    try {
      ArtifactContextBuilder artifactBuilder = getArtifactContextBuilder().setArtifactName(getArtifactName())
          .setDataFolderName(getDescriptor().getDataFolderName())
          .setArtifactPlugins(artifactPlugins)
          .setExecutionClassloader(deploymentClassLoader.getClassLoader())
          .setArtifactInstallationDirectory(getArtifactInstallationDirectory())
          .setExtensionModelLoaderRepository(extensionModelLoaderManager)
          .setArtifactType(DOMAIN)
          .setEnableLazyInit(lazy)
          .setDisableXmlValidations(disableXmlValidations)
          .setClassLoaderRepository(classLoaderRepository)
          .setProperties(ofNullable(resolveDeploymentProperties(descriptor.getDataFolderName(),
                                                                descriptor.getDeploymentProperties())))
          .setServiceRepository(serviceRepository)
          .setRuntimeComponentBuildingDefinitionProvider(runtimeComponentBuildingDefinitionProvider)
          .setRuntimeLockFactory(runtimeLockFactory);

      if (!descriptor.getConfigResources().isEmpty()) {
        validateConfigurationFileDoNotUsesCoreNamespace();
        artifactBuilder
            .setConfigurationFiles(descriptor.getConfigResources().toArray(new String[descriptor.getConfigResources().size()]));
      }

      if (muleContextListener != null) {
        artifactBuilder.setMuleContextListener(muleContextListener);
      }
      artifactContext = artifactBuilder.build();
    } catch (Exception e) {
      // log it here so it ends up in app log, sys log will only log a message without stacktrace
      LOGGER.error(e.getMessage(), getRootCause(e));
      throw new DeploymentInitException(createStaticMessage(getRootCauseMessage(e)), e);
    }
  }

  private void validateConfigurationFileDoNotUsesCoreNamespace() throws FileNotFoundException {
    for (String configResourceFile : descriptor.getConfigResources()) {
      try (Scanner scanner = new Scanner(getArtifactClassLoader().getClassLoader().getResourceAsStream(configResourceFile))) {
        while (scanner.hasNextLine()) {
          final String lineFromFile = scanner.nextLine();
          if (lineFromFile.contains("<mule ")) {
            throw new MuleRuntimeException(createStaticMessage("Domain configuration file can not be created using core namespace. Use mule-domain namespace instead."));
          }
        }
      }
    }
  }

  @Override
  public void start() {
    try {
      if (this.artifactContext != null) {
        try {
          this.artifactContext.getMuleContext().start();
        } catch (MuleException e) {
          LOGGER.error(null, getRootCause(e));
          throw new DeploymentStartException(createStaticMessage(getRootCauseMessage(e)), e);
        }
      }
      // null CCL ensures we log at 'system' level
      // TODO create a more usable wrapper for any logger to be logged at sys level
      Thread currentThread = currentThread();
      final ClassLoader originalClassLoader = currentThread().getContextClassLoader();
      currentThread.setContextClassLoader(null);
      try {
        DomainStartedSplashScreen splashScreen = new DomainStartedSplashScreen();
        splashScreen.createMessage(descriptor);
        log(splashScreen.toString());
      } finally {
        currentThread.setContextClassLoader(originalClassLoader);
      }
    } catch (Exception e) {
      throw new DeploymentStartException(createStaticMessage("Failure trying to start domain " + getArtifactName()), e);
    }
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
  public DomainDescriptor getDescriptor() {
    return descriptor;
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
  public boolean containsSharedResources() {
    return this.artifactContext != null;
  }

  /**
   * Method created for testing purposes.
   *
   * @return the muleContextFactory for creating the mule context of the domain
   */
  protected ArtifactContextBuilder getArtifactContextBuilder() {
    return newBuilder();
  }

  /**
   * Method created for testing purposes.
   *
   * @return the installation directory.
   */
  protected File getArtifactInstallationDirectory() {
    return descriptor.getArtifactLocation();
  }

}
