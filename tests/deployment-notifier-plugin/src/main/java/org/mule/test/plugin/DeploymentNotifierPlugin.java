/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Collections.synchronizedSet;
import static org.slf4j.LoggerFactory.getLogger;

import com.mulesoft.mule.runtime.module.plugin.api.MulePlugin;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Mule deployment plugin that use {@link DeploymentServerSocket} server to answer client's requests
 * about deployment status over apps and domains.
 *
 * @since 4.0
 */
public class DeploymentNotifierPlugin implements MulePlugin {

  private static final Logger LOGGER = getLogger(DeploymentNotifierPlugin.class);
  private static String DEPLOYMENT_SERVER_SOCKET_PORT_PROPERTY = "mule.test.deployment.server.socket.port";
  private static int SERVER_PORT = parseInt(getProperty(DEPLOYMENT_SERVER_SOCKET_PORT_PROPERTY));

  protected File directory;

  @Inject
  private DeploymentService deploymentService;

  private DeploymentServerSocket serverSocket;

  private Set<String> deployedArtifacts = synchronizedSet(new HashSet<>());

  private Set<String> deployedDomains = synchronizedSet(new HashSet<>());

  @Override
  public void setWorkingDirectory(File directory) {
    this.directory = directory;
  }

  @Override
  public boolean isDisabledOnEnvironment() {
    return false;
  }

  @Override
  public void dispose() {

  }

  @Override
  public void initialise() throws InitialisationException {
    serverSocket = new DeploymentServerSocket(SERVER_PORT, this);
  }

  @Override
  public void start() throws MuleException {
    try {
      LOGGER.info("Starting DeploymentServerSocket...");
      serverSocket.start();

    } catch (IOException e) {
      stopServerQuietly();
      throw new RuntimeException(e);
    }

    LOGGER.info("Registering deployment listener ...");
    deploymentService.addDeploymentListener(new DeploymentListener() {

      @Override
      public void onDeploymentSuccess(String artifactName) {
        LOGGER.info(format("Deployment success:  %s", artifactName));
        deployedArtifacts.add(artifactName);
      }

      @Override
      public void onUndeploymentSuccess(String artifactName) {
        LOGGER.info(format("Undeployment success: %s", artifactName));
        deployedArtifacts.remove(artifactName);
      }
    });

    LOGGER.info("Registering domain deployment listener ...");
    deploymentService.addDomainDeploymentListener(new DeploymentListener() {

      @Override
      public void onDeploymentSuccess(String artifactName) {
        LOGGER.info(format("[DOMAIN] Deployment success:  %s", artifactName));
        deployedDomains.add(artifactName);
      }

      @Override
      public void onUndeploymentSuccess(String artifactName) {
        LOGGER.info(format("[DOMAIN] Undeployment success: %s", artifactName));
        deployedDomains.remove(artifactName);
      }
    });
  }

  @Override
  public void stop() throws MuleException {
    stopServerQuietly();
  }

  private void stopServerQuietly() {
    if (serverSocket != null) {
      try {
        LOGGER.info("Stopping DeploymentServerSocket ...");
        serverSocket.stop();
      } catch (Exception e) {
        // Do nothing.
        LOGGER.warn("Failed stop DeploymentServerSocket", e);
      }
    }
  }

  public boolean isDeployed(String artifactName) {
    return deployedArtifacts.contains(artifactName);
  }

  public boolean isDomainDeployed(String artifactName) {
    return deployedDomains.contains(artifactName);
  }
}
