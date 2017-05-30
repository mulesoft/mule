/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.util.SplashScreen.miniSplash;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.internal.util.ExceptionUtils;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.DeploymentStopException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleDomain implements Domain {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  private final DomainDescriptor descriptor;
  private final ServiceRepository serviceRepository;
  private MuleContextListener muleContextListener;
  private ArtifactClassLoader deploymentClassLoader;
  private final ClassLoaderRepository classLoaderRepository;

  private File configResourceFile;
  private ArtifactContext artifactContext;

  public DefaultMuleDomain(DomainDescriptor descriptor, ArtifactClassLoader deploymentClassLoader,
                           ClassLoaderRepository classLoaderRepository, ServiceRepository serviceRepository) {
    this.deploymentClassLoader = deploymentClassLoader;
    this.classLoaderRepository = classLoaderRepository;
    this.descriptor = descriptor;
    this.serviceRepository = serviceRepository;
    refreshClassLoaderAndLoadConfigResourceFile();
  }

  private void refreshClassLoaderAndLoadConfigResourceFile() {
    URL resource = deploymentClassLoader.findLocalResource(DOMAIN_CONFIG_FILE_LOCATION);
    if (resource != null) {
      try {
        this.configResourceFile = new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Unable to find config resource file: " + resource.getFile());
      }
    }
  }

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
  public void install() {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(String.format("New domain '%s'", getArtifactName())));
    }
    refreshClassLoaderAndLoadConfigResourceFile();
  }


  @Override
  public void init() {
    doInit(false);
  }

  @Override
  public void lazyInit() {
    doInit(true);
  }

  public void doInit(boolean lazy) {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(String.format("Initializing domain '%s'", getArtifactName())));
    }

    try {
      if (this.configResourceFile != null) {
        validateConfigurationFileDoNotUsesCoreNamespace();

        ArtifactContextBuilder artifactBuilder = newBuilder().setArtifactName(getArtifactName())
            .setExecutionClassloader(deploymentClassLoader.getClassLoader())
            .setArtifactInstallationDirectory(new File(MuleContainerBootstrapUtils.getMuleDomainsDir(), getArtifactName()))
            .setConfigurationFiles(new String[] {this.configResourceFile.getAbsolutePath()}).setArtifactType(DOMAIN)
            .setEnableLazyInit(lazy).setClassLoaderRepository(classLoaderRepository).setServiceRepository(serviceRepository);

        if (muleContextListener != null) {
          artifactBuilder.setMuleContextListener(muleContextListener);
        }
        artifactContext = artifactBuilder.build();
      }
    } catch (Exception e) {
      // log it here so it ends up in app log, sys log will only log a message without stacktrace
      logger.error(null, ExceptionUtils.getRootCause(e));
      throw new DeploymentInitException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
    }
  }

  private void validateConfigurationFileDoNotUsesCoreNamespace() throws FileNotFoundException {
    Scanner scanner = null;
    try {
      scanner = new Scanner(configResourceFile);
      while (scanner.hasNextLine()) {
        final String lineFromFile = scanner.nextLine();
        if (lineFromFile.contains("<mule ")) {
          throw new MuleRuntimeException(CoreMessages
              .createStaticMessage("Domain configuration file can not be created using core namespace. Use mule-domain namespace instead."));
        }
      }
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }
  }

  private ConfigurationBuilder createConfigurationBuilder() {
    try {
      return (ConfigurationBuilder) ClassUtils
          .instanciateClass("org.mule.runtime.config.spring.SpringXmlDomainConfigurationBuilder",
                            new Object[] {getResourceFiles()[0].getName()}, deploymentClassLoader.getClassLoader());
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public void start() {
    try {
      if (this.artifactContext != null) {
        try {
          this.artifactContext.getMuleContext().start();
        } catch (MuleException e) {
          logger.error(null, ExceptionUtils.getRootCause(e));
          throw new DeploymentStartException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
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
      throw new DeploymentStartException(CoreMessages.createStaticMessage("Failure trying to start domain " + getArtifactName()),
                                         e);
    }
  }

  @Override
  public void stop() {
    try {
      if (logger.isInfoEnabled()) {
        logger.info(miniSplash(String.format("Stopping domain '%s'", getArtifactName())));
      }
      if (this.artifactContext != null) {
        this.artifactContext.getMuleContext().stop();
      }
    } catch (Exception e) {
      throw new DeploymentStopException(CoreMessages.createStaticMessage("Failure trying to stop domain " + getArtifactName()),
                                        e);
    }
  }

  @Override
  public void dispose() {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(String.format("Disposing domain '%s'", getArtifactName())));
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
    return configResourceFile == null ? new File[0] : new File[] {configResourceFile};
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
      throw new DeploymentInitException(CoreMessages
          .createStaticMessage("Failure trying to initialise domain " + getArtifactName()), e);
    }
  }

  @Override
  public boolean containsSharedResources() {
    return this.artifactContext != null;
  }
}
