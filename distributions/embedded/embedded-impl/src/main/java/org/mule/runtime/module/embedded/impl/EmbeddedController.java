/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.impl;

import static java.lang.System.setProperty;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getConfFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServerPluginsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderModelLoader.ADD_TEST_DEPENDENCIES_KEY;
import static org.mule.runtime.module.embedded.impl.SerializationUtils.deserialize;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.embedded.api.ArtifactConfiguration;
import org.mule.runtime.module.embedded.api.ContainerInfo;
import org.mule.runtime.module.launcher.MuleContainer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import net.lingala.zip4j.core.ZipFile;

/**
 * Controller class for the runtime. It spin ups a new container instance using a temporary folder and dynamically loading the
 * container libraries.
 * <p>
 * Invoked by reflection
 *
 * @since 4.0
 */
public class EmbeddedController {

  private ContainerInfo containerInfo;
  private ArtifactClassLoader containerClassLoader;
  private MuleContainer muleContainer;

  public EmbeddedController(byte[] serializedContainerInfo)
      throws IOException, ClassNotFoundException {
    containerInfo = deserialize(serializedContainerInfo);
  }

  /**
   * Invoked by reflection
   */
  public void start() throws Exception {
    setUpEnvironment();
  }

  public void deployApplication(byte[] serializedArtifactConfiguration) throws IOException, ClassNotFoundException {
    ArtifactConfiguration artifactConfiguration = deserialize(serializedArtifactConfiguration);
    withSystemProperties(artifactConfiguration.getDeploymentConfiguration().getArtifactProperties(), () -> {
      try {
        if (artifactConfiguration.getDeploymentConfiguration().enableTestDependencies()) {
          setProperty(ADD_TEST_DEPENDENCIES_KEY, "true");
        }
        muleContainer.getDeploymentService().deploy(artifactConfiguration.getApplicationLocation().toURI());
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        setProperty(ADD_TEST_DEPENDENCIES_KEY, "false");
      }
    });
  }

  public void undeployApplication(byte[] serializedApplicationName) throws IOException, ClassNotFoundException {
    String applicationName = deserialize(serializedApplicationName);
    muleContainer.getDeploymentService().undeploy(applicationName);
  }

  // TODO MULE-10392: To be removed once we have methods to deploy with properties
  private void withSystemProperties(Map<String, String> systemProperties, Runnable runnable) {
    Map<String, String> previousPropertyValues = new HashMap<>();
    systemProperties.entrySet().stream().forEach(entry -> {
      String previousValue = System.getProperty(entry.getKey());
      if (previousValue != null) {
        previousPropertyValues.put(entry.getKey(), entry.getValue());
      }
      setProperty(entry.getKey(), entry.getValue());
    });
    try {
      runnable.run();
    } catch (Exception e) {
      previousPropertyValues.entrySet().stream().forEach(entry -> {
        setProperty(entry.getKey(), entry.getValue());
      });
    }
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
      muleContainer.stop();
      muleContainer.getContainerClassLoader().dispose();
    });
  }

  private void setUpEnvironment() throws IOException {
    // Disable log4j2 JMX MBeans since it will fail when trying to recreate the container
    setProperty("log4j2.disable.jmx", "true");

    setProperty(MULE_HOME_DIRECTORY_PROPERTY, containerInfo.getContainerBaseFolder().getPath());
    getDomainsFolder().mkdirs();
    getDomainFolder("default").mkdirs();
    getServicesFolder().mkdirs();
    getServerPluginsFolder().mkdirs();
    getConfFolder().mkdirs();
    getAppsFolder().mkdirs();

    // this is used to signal that we are running in embedded mode.
    // Class loader model loader will not use try to use the container repository.
    setProperty("mule.mode.embedded", "true");

    for (URL url : containerInfo.getServices()) {
      File originalFile = toFile(url);
      File destinationFile = new File(getServicesFolder(), getName(originalFile.getPath()));
      copyFile(originalFile, destinationFile);
    }
    containerInfo.getServerPlugins().stream().forEach(serverPluginUrl -> {
      File originalFile = toFile(serverPluginUrl);
      File destinationFile = new File(getServerPluginsFolder(), getName(originalFile.getPath()).replace(".zip", ""));
      try {
        ZipFile zipFile = new ZipFile(originalFile);
        zipFile.extractAll(destinationFile.getAbsolutePath());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    muleContainer = new MuleContainer(new String[0]);
    containerClassLoader = muleContainer.getContainerClassLoader();
    executeWithinContainerClassLoader(() -> {
      try {
        // Do not register shutdown hook since it will try to kill the JVM
        muleContainer.start(false);
      } catch (MuleException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  /**
   * Interface for running tasks within the container class loader.
   */
  @FunctionalInterface
  interface ContainerTask {

    void run() throws Exception;

  }

}
