/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.util;

import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToCreate;
import static org.mule.runtime.core.api.util.FileUtils.newFile;

import static java.io.File.separator;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common logic to create and persist a file containing deployment properties for each app/domain deployed/redeployed.
 */
public class DeploymentPropertiesUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentPropertiesUtils.class);

  private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";

  private static final String FLOWS_DEPLOYMENT_PROPERTIES_FILE_NAME = "flows.deployment.properties";

  private static final String DEPLOYMENT_PROPERTIES_DIRECTORY = "deployment-properties";

  private static final String ARTIFACT_STATUS_DEPLOYMENT_PROPERTIES_FILE_NAME = "artifact.status.deployment.properties";

  /**
   * This method resolves the deploymentProperties for a new deploy/redeploy considering the new deployment properties passed by
   * the user as parameter and the deployment properties persisted in a previous deploy. The new deployment properties are used
   * and persisted in .mule/app/deployment-properties/<fileName>.
   *
   * @param artifactName         name of the artifact.
   * @param deploymentProperties deployment properties set in the new deploy/redeploy as parameters.
   * @param fileName             name of the file where the deployment properties are persisted.
   * @return deployment properties
   * @throws IOException
   */
  public static Properties resolveDeploymentProperties(String artifactName, Optional<Properties> deploymentProperties,
                                                       String fileName)
      throws IOException {
    Properties properties = deploymentProperties.orElse(new Properties());
    setPersistedProperties(artifactName, properties, fileName);
    return properties;
  }

  public static Optional<Properties> getPersistedProperties(String artifactName, String fileName) {
    try {
      String deploymentPropertiesPath = getDeploymentPropertiesPath(artifactName);
      return of(getDeploymentProperties(deploymentPropertiesPath, fileName));
    } catch (IOException e) {
      LOGGER.atError()
          .setCause(e)
          .log("Failed to load persisted deployment property for artifact {}", artifactName);
      return empty();
    }
  }

  public static Optional<Properties> getPersistedDeploymentProperties(String artifactName) {
    return getPersistedProperties(artifactName, DEPLOYMENT_PROPERTIES_FILE_NAME);
  }

  public static Optional<Properties> getPersistedFlowDeploymentProperties(String artifactName) {
    return getPersistedProperties(artifactName, FLOWS_DEPLOYMENT_PROPERTIES_FILE_NAME);
  }

  public static Optional<Properties> getPersistedArtifactStatusDeploymentProperties(String artifactName) {
    return getPersistedProperties(artifactName, ARTIFACT_STATUS_DEPLOYMENT_PROPERTIES_FILE_NAME);
  }

  public static void setPersistedProperties(String artifactName, Properties properties, String fileName) throws IOException {
    String deploymentPropertiesPath = getDeploymentPropertiesPath(artifactName);
    initDeploymentPropertiesDirectory(deploymentPropertiesPath);
    persistDeploymentPropertiesFile(deploymentPropertiesPath, properties, fileName);
  }

  public static void setPersistedDeploymentProperties(String artifactName, Properties properties)
      throws IOException {
    setPersistedProperties(artifactName, properties, DEPLOYMENT_PROPERTIES_FILE_NAME);
  }

  public static void setPersistedFlowDeploymentProperties(String artifactName, Properties properties)
      throws IOException {
    setPersistedProperties(artifactName, properties, FLOWS_DEPLOYMENT_PROPERTIES_FILE_NAME);
  }

  public static void setPersistedArtifactStatusDeploymentProperties(String artifactName, Properties properties)
      throws IOException {
    setPersistedProperties(artifactName, properties, ARTIFACT_STATUS_DEPLOYMENT_PROPERTIES_FILE_NAME);
  }

  private static String getDeploymentPropertiesPath(String artifactName) {
    File file = new File(getExecutionFolder(), artifactName);
    String workingDirectory = file.getAbsolutePath();
    return workingDirectory + separator + DEPLOYMENT_PROPERTIES_DIRECTORY;
  }

  /**
   * Gets the deployment properties from the deploymentPropertiesPath. The file that contains the properties is
   * deployment.properties.
   *
   * @param deploymentPropertiesPath the path where the deployment properties are located
   * @return deployment properties
   * @throws IOException
   */
  private static Properties getDeploymentProperties(String deploymentPropertiesPath, String fileName) throws IOException {
    File configFile = new File(deploymentPropertiesPath + separator + fileName);
    Properties props = new Properties();

    if (!configFile.exists()) {
      return props;
    }

    try (FileReader reader = new FileReader(configFile)) {
      props.load(reader);
    }

    return props;
  }

  /**
   * Initialises the directory for the deployment properties in case it does not exist.
   *
   * @param deploymentPropertiesPath the path to persist the deployment properties.
   */
  private static void initDeploymentPropertiesDirectory(String deploymentPropertiesPath) {

    File deploymentPropertiesDirectory = newFile(deploymentPropertiesPath);
    if (!deploymentPropertiesDirectory.exists()) {
      createDeploymentPropertiesDirectory(deploymentPropertiesDirectory);
    }
  }

  /**
   * Creates the deployment properties directory.
   *
   * @param deploymentPropertiesDirectory path to create
   */
  private static synchronized void createDeploymentPropertiesDirectory(File deploymentPropertiesDirectory) {
    if (!deploymentPropertiesDirectory.exists() && !deploymentPropertiesDirectory.mkdirs()) {
      I18nMessage message = failedToCreate("deployment properties directory "
          + deploymentPropertiesDirectory.getAbsolutePath());
      throw new MuleRuntimeException(message);
    }
  }

  private static void persistDeploymentPropertiesFile(String deploymentPropertiesPath, Properties deploymentProperties,
                                                      String fileName)
      throws IOException {
    File deploymentPropertiesFile = new File(deploymentPropertiesPath, fileName);
    try (FileWriter fileWriter = new FileWriter(deploymentPropertiesFile.getAbsolutePath(), false)) {
      deploymentProperties.store(fileWriter, "deployment properties");
    }
  }

  /**
   * This method resolves the deploymentProperties for a certain artifact. There is one deployment.properties file for each
   * artifact (domain/app).
   *
   * @param artifactName         name of the artifact.
   * @param deploymentProperties deployment properties set in the new deploy/redeploy as parameters.
   * @return deployment properties
   * @throws IOException
   */
  public static Properties resolveDeploymentProperties(String artifactName, Optional<Properties> deploymentProperties)
      throws IOException {
    return resolveDeploymentProperties(artifactName, deploymentProperties, DEPLOYMENT_PROPERTIES_FILE_NAME);
  }

  /**
   * This method resolves the deploymentProperties for a flow of a certain app. There is one flow.deployment.properties file for
   * all flows of an app.
   *
   * @param appName              name of the application that contains the flows.
   * @param deploymentProperties deployment properties set in the new deploy/redeploy as parameters.
   * @return deployment properties
   * @throws IOException
   */
  public static Properties resolveFlowDeploymentProperties(String appName, Optional<Properties> deploymentProperties)
      throws IOException {
    return resolveDeploymentProperties(appName, deploymentProperties, FLOWS_DEPLOYMENT_PROPERTIES_FILE_NAME);
  }

  /**
   * This method resolves the statusProperties for the status (started, stopped) of a certain artifact. There is one
   * artifact.status.deployment.properties file for each artifact (domain/app).
   *
   * @param artifactName     name of the artifact.
   * @param statusProperties status deployment properties set in the new deploy/redeploy as parameters.
   * @return deployment properties
   * @throws IOException
   */
  public static Properties resolveArtifactStatusDeploymentProperties(String artifactName, Optional<Properties> statusProperties)
      throws IOException {
    return resolveDeploymentProperties(artifactName, statusProperties, ARTIFACT_STATUS_DEPLOYMENT_PROPERTIES_FILE_NAME);
  }
}
