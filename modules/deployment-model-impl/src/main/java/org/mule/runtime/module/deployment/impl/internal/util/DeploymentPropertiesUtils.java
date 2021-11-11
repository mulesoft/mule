/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.util;

import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.core.api.util.FileUtils.newFile;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToCreate;
import static java.io.File.separator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Common logic to create and persist a file containing deployment properties for each app/domain deployed/redeployed.
 */
public class DeploymentPropertiesUtils {

  private static final String DEPLOYMENT_PROPERTIES_FILE_NAME = "deployment.properties";

  private static final String FLOWS_DEPLOYMENT_PROPERTIES_FILE_NAME = "flows.deployment.properties";

  private static final String DEPLOYMENT_PROPERTIES_DIRECTORY = "deployment-properties";

  private static final String ARTIFACT_STATUS_DEPLOYMENT_PROPERTIES_FILE_NAME = "artifact.status.deployment.properties";

  /**
   * This method resolves the deploymentProperties for a new deploy/redeploy considering the new deployment properties passed by
   * the user as parameter and the deployment properties persisted in a previous deploy. In case no new deployment properties are
   * passed, the previous persisted properties are returned. Otherwise, the new deployment properties are used and persisted in
   * .mule/app/deployment-properties/<fileName>.
   *
   * @param artifactName         name of the artifact.
   * @param deploymentProperties deployment properties set in the new deploy/redeploy as parameters.
   * @param fileName             name of the file where the deployment properties are persisted.
   *
   * @return deployment properties
   * @throws IOException
   */
  public static Properties resolveDeploymentProperties(String artifactName, Optional<Properties> deploymentProperties,
                                                       String fileName)
      throws IOException {
    File file = new File(getExecutionFolder(), artifactName);
    String workingDirectory = file.getAbsolutePath();
    String deploymentPropertiesPath = workingDirectory + separator + DEPLOYMENT_PROPERTIES_DIRECTORY;

    if (!deploymentProperties.isPresent()) {
      return getDeploymentProperties(deploymentPropertiesPath, fileName);
    }

    initDeploymentPropertiesDirectory(deploymentPropertiesPath);
    persistDeploymentPropertiesFile(deploymentPropertiesPath, deploymentProperties.get(), fileName);

    return deploymentProperties.get();
  }

  /**
   * Gets the deployment properties from the deploymentPropertiesPath. The file that contains the properties is
   * deployment.properties.
   *
   * @param deploymentPropertiesPath the path where the deployment properties are located
   * @return deployment properties
   *
   * @throws IOException
   */
  private static Properties getDeploymentProperties(String deploymentPropertiesPath, String fileName) throws IOException {
    File configFile = new File(deploymentPropertiesPath + separator + fileName);
    Properties props = new Properties();

    if (!configFile.exists()) {
      return props;
    }

    FileReader reader = new FileReader(configFile);

    props.load(reader);

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
    FileWriter fileWriter = new FileWriter(deploymentPropertiesFile.getAbsolutePath(), false);
    deploymentProperties.store(fileWriter, "deployment properties");
    fileWriter.close();
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
