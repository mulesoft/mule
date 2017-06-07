/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.impl;

import static java.lang.System.setProperty;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;
import static org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderModelLoader.ADD_TEST_DEPENDENCIES_KEY;
import static org.mule.runtime.module.embedded.impl.SerializationUtils.deserialize;
import static org.mule.runtime.module.embedded.internal.MavenUtils.createModelFromPom;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.embedded.api.ApplicationConfiguration;
import org.mule.runtime.module.embedded.api.ContainerInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class for the runtime. It spin ups a new container instance using a temporary folder and dynamically loading the
 * container libraries.
 * <p>
 * Invoked by reflection
 *
 * @since 4.0
 */
public class EmbeddedController {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedController.class);
  private ApplicationConfiguration applicationConfiguration;
  private ContainerInfo containerInfo;
  private Application application;
  private ArtifactClassLoader containerClassLoader;
  private MuleArtifactResourcesRegistry artifactResourcesRegistry;

  public EmbeddedController(byte[] serializedContainerInfo, byte[] serializedAppConfiguration)
      throws IOException, ClassNotFoundException {
    containerInfo = deserialize(serializedContainerInfo);
    applicationConfiguration = deserialize(serializedAppConfiguration);
  }

  /**
   * Invoked by reflection
   */
  public void start() throws Exception {
    setUpEnvironment();
    createApplication();

    application.init();
    application.start();
  }

  private void createApplication()
      throws Exception {

    for (Map.Entry<String, String> applicationPropertiesEntry : applicationConfiguration.getDeploymentConfiguration()
        .getArtifactProperties().entrySet()) {
      setProperty(applicationPropertiesEntry.getKey(), applicationPropertiesEntry.getValue());
    }

    // this is used to signal that we are running in embedded mode.
    // Class loader model loader will not use try to use the container repository.
    setProperty("mule.mode.embedded", "true");
    if (applicationConfiguration.getDeploymentConfiguration().enableTestDependencies()) {
      setProperty(ADD_TEST_DEPENDENCIES_KEY, "true");
    }

    File applicationLocation = applicationConfiguration.getApplicationLocation();
    File appsFolder = new File(MuleFoldersUtil.getAppsFolder().getAbsolutePath());
    File deploymentApplicationLocation = new File(appsFolder, applicationLocation.getName().replace(".jar", ""));
    if (applicationLocation.getName().endsWith(".jar")) {
      new ZipFile(applicationLocation).extractAll(appsFolder.getAbsolutePath());
    } else {
      copyDirectory(applicationLocation, deploymentApplicationLocation);
    }

    File containerFolder = new File(containerInfo.getContainerBaseFolder().getPath());
    File servicesFolder = new File(containerFolder, "services");
    for (URL url : containerInfo.getServices()) {
      File originalFile = toFile(url);
      File destinationFile = new File(servicesFolder, FilenameUtils.getName(originalFile.getPath()));
      copyFile(originalFile, destinationFile);
    }

    artifactResourcesRegistry = new MuleArtifactResourcesRegistry.Builder().build();
    containerClassLoader = this.artifactResourcesRegistry.getContainerClassLoader();

    executeWithinContainerClassLoader(() -> {
      try {
        try {
          artifactResourcesRegistry.getServiceManager().start();
          artifactResourcesRegistry.getExtensionModelLoaderManager().start();
        } catch (MuleException e) {
          throw new IllegalStateException(e);
        }

        ApplicationDescriptor applicationDescriptor =
            artifactResourcesRegistry.getApplicationDescriptorFactory().create(deploymentApplicationLocation);
        artifactResourcesRegistry.getDomainFactory().createArtifact(createDefaultDomainDir());

        this.application = artifactResourcesRegistry.getApplicationFactory().createAppFrom(applicationDescriptor);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public void executeWithinContainerClassLoader(ContainerTask task) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(containerClassLoader.getClassLoader());
      task.run();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  /**
   * Invoked by reflection
   */
  public void stop() {
    executeWithinContainerClassLoader(() -> {
      deleteTree(new File(containerInfo.getContainerBaseFolder().getPath()));
      try {
        application.stop();
      } catch (Exception e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("failure stopping application", e);
        }
      }
      try {
        application.dispose();
      } catch (Exception e) {
        LOGGER.debug("failure disposing application", e);
      }
    });
    artifactResourcesRegistry = null;
  }

  private void setUpEnvironment() {
    setProperty(MULE_HOME_DIRECTORY_PROPERTY, containerInfo.getContainerBaseFolder().getPath());
    getDomainsFolder().mkdirs();
    getServicesFolder().mkdirs();
  }

  private File createDefaultDomainDir() {
    File containerFolder = new File(containerInfo.getContainerBaseFolder().getPath());
    File defaultDomainFolder = new File(new File(containerFolder, "domains"), "default");
    if (!defaultDomainFolder.mkdirs()) {
      throw new RuntimeException("Could not create default domain directory in " + defaultDomainFolder.getAbsolutePath());
    }
    return defaultDomainFolder;
  }

  /**
   * Interface for running tasks within the container class loader.
   */
  @FunctionalInterface
  interface ContainerTask {

    void run() throws Exception;

  }

}
