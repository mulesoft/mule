/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.getMuleDomainsDir;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.DeploymentStopException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.service.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

public class DefaultMuleDomain implements Domain {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  private final DomainDescriptor descriptor;
  private final ServiceRepository serviceRepository;
  private final List<ArtifactPlugin> artifactPlugins;
  private final ExtensionModelLoaderManager extensionModelLoaderManager;
  private final ClassLoaderRepository classLoaderRepository;
  private final ArtifactClassLoader deploymentClassLoader;

  private MuleContextListener muleContextListener;
  private ArtifactContext artifactContext;

  public DefaultMuleDomain(DomainDescriptor descriptor, ArtifactClassLoader deploymentClassLoader,
                           ClassLoaderRepository classLoaderRepository, ServiceRepository serviceRepository,
                           List<ArtifactPlugin> artifactPlugins, ExtensionModelLoaderManager extensionModelLoaderManager) {
    this.deploymentClassLoader = deploymentClassLoader;
    this.classLoaderRepository = classLoaderRepository;
    this.descriptor = descriptor;
    this.serviceRepository = serviceRepository;
    this.artifactPlugins = artifactPlugins;
    this.extensionModelLoaderManager = extensionModelLoaderManager;
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
  public MuleContext getMuleContext() {
    return artifactContext != null ? artifactContext.getMuleContext() : null;
  }

  @Override
  public File getLocation() {
    return descriptor.getArtifactLocation();
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
  public ValueProviderService getValueProviderService() {
    return artifactContext.getValueProviderService();
  }

  @Override
  public List<ArtifactPlugin> getArtifactPlugins() {
    return artifactPlugins;
  }

  @Override
  public void install() {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("New domain '%s'", getArtifactName())));
    }
  }

  @Override
  public void init() {
    doInit(false);
  }

  @Override
  public void lazyInit() {
    doInit(true);
  }

  public void doInit(boolean lazy) throws DeploymentInitException {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("Initializing domain '%s'", getArtifactName())));
    }

    try {
      ArtifactContextBuilder artifactBuilder = newBuilder().setArtifactName(getArtifactName())
          .setArtifactPlugins(artifactPlugins)
          .setExecutionClassloader(deploymentClassLoader.getClassLoader())
          .setArtifactInstallationDirectory(new File(getMuleDomainsDir(), getArtifactName()))
          .setExtensionModelLoaderRepository(extensionModelLoaderManager)
          .setArtifactType(DOMAIN)
          .setEnableLazyInit(lazy)
          .setClassLoaderRepository(classLoaderRepository)
          .setServiceRepository(serviceRepository);

      if (descriptor.getAbsoluteResourcePaths().length > 0) {
        validateConfigurationFileDoNotUsesCoreNamespace();
        artifactBuilder.setConfigurationFiles(descriptor.getAbsoluteResourcePaths());
      }

      if (muleContextListener != null) {
        artifactBuilder.setMuleContextListener(muleContextListener);
      }
      artifactContext = artifactBuilder.build();
    } catch (Exception e) {
      // log it here so it ends up in app log, sys log will only log a message without stacktrace
      logger.error(null, getRootCause(e));
      throw new DeploymentInitException(createStaticMessage(getRootCauseMessage(e)), e);
    }
  }

  private void validateConfigurationFileDoNotUsesCoreNamespace() throws FileNotFoundException {
    for (String configResourceFile : descriptor.getAbsoluteResourcePaths()) {
      try (Scanner scanner = new Scanner(configResourceFile)) {
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
          logger.error(null, getRootCause(e));
          throw new DeploymentStartException(createStaticMessage(getRootCauseMessage(e)), e);
        }
      }
      // null CCL ensures we log at 'system' level
      // TODO create a more usable wrapper for any logger to be logged at sys level
      withContextClassLoader(null, () -> {
        DomainStartedSplashScreen splashScreen = new DomainStartedSplashScreen();
        splashScreen.createMessage(descriptor);
        logger.info(splashScreen.toString());
      });
    } catch (Exception e) {
      throw new DeploymentStartException(createStaticMessage("Failure trying to start domain " + getArtifactName()), e);
    }
  }

  @Override
  public void stop() {
    try {
      if (logger.isInfoEnabled()) {
        logger.info(miniSplash(format("Stopping domain '%s'", getArtifactName())));
      }
      if (this.artifactContext != null) {
        this.artifactContext.getMuleContext().stop();
      }
    } catch (Exception e) {
      throw new DeploymentStopException(createStaticMessage("Failure trying to stop domain " + getArtifactName()), e);
    }
  }

  @Override
  public void dispose() {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("Disposing domain '%s'", getArtifactName())));
    }
    if (this.artifactContext != null) {
      this.artifactContext.getMuleContext().dispose();
    }
    this.deploymentClassLoader.dispose();
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
    return descriptor.getConfigResourcesFile();
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return deploymentClassLoader;
  }

  public void initialise() {
    try {
      if (this.artifactContext != null) {
        this.artifactContext.getMuleContext().initialise();
      }
    } catch (InitialisationException e) {
      throw new DeploymentInitException(createStaticMessage("Failure trying to initialise domain " + getArtifactName()), e);
    }
  }

  @Override
  public boolean containsSharedResources() {
    return this.artifactContext != null;
  }
}
