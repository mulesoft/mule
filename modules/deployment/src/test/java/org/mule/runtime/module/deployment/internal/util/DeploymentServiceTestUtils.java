/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.util;

import org.mule.runtime.module.deployment.api.DeploymentService;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility class used to avoid skipping actions in the MuleDeploymentService#executeSynchronized method. We avoid it by acquiring
 * the lock before calling the actual method.
 */
public final class DeploymentServiceTestUtils {

  private DeploymentServiceTestUtils() {}

  /**
   * Undeploys an application from the mule container
   *
   * @param appName name of the application to undeploy
   */
  public static void undeploy(DeploymentService delegate, String appName) {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.undeploy(appName);
    }
  }

  /**
   * Deploys an application bundled as a zip from the given URL to the mule container
   *
   * @param appArchiveUri location of the zip application file
   * @throws IOException
   */
  public static void deploy(DeploymentService delegate, URI appArchiveUri) throws IOException {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.deploy(appArchiveUri);
    }
  }

  /**
   * Deploys an application bundled as a zip from the given URL to the mule container and applies the provided properties.
   *
   * @param appArchiveUri location of the zip application file
   * @param appProperties map of properties to include
   * @throws IOException
   */
  public static void deploy(DeploymentService delegate, URI appArchiveUri, Properties appProperties) throws IOException {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.deploy(appArchiveUri, appProperties);
    }
  }

  /**
   * Undeploys and redeploys an application
   *
   * @param artifactName then name of the application to redeploy
   */
  public static void redeploy(DeploymentService delegate, String artifactName) {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.redeploy(artifactName);
    }
  }

  /**
   * Undeploys and redeploys an application including the provided appProperties.
   *
   * @param artifactName  then name of the application to redeploy
   * @param appProperties map of properties to include
   */
  public static void redeploy(DeploymentService delegate, String artifactName, Properties appProperties) {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.redeploy(artifactName, appProperties);
    }
  }

  /**
   * Undeploys and redeploys an application using a new artifact URI and including the provided appProperties.
   *
   * @param archiveUri    location of the application file
   * @param appProperties map of properties to include
   */
  public static void redeploy(DeploymentService delegate, URI archiveUri, Properties appProperties) throws IOException {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.redeploy(archiveUri, appProperties);
    }
  }

  /**
   * Undeploys and redeploys an application using a new artifact URI.
   *
   * @param archiveUri location of the application file
   */
  public static void redeploy(DeploymentService delegate, URI archiveUri) throws IOException {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.redeploy(archiveUri);
    }
  }

  /**
   * Undeploys a domain from the mule container
   *
   * @param domainName name of the domain to undeploy
   */
  public static void undeployDomain(DeploymentService delegate, String domainName) {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.undeployDomain(domainName);
    }
  }

  /**
   * Deploys a domain artifact from the given URL to the mule container
   *
   * @param domainArchiveUri location of the domain file
   * @throws IOException
   */
  public static void deployDomain(DeploymentService delegate, URI domainArchiveUri) throws IOException {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.deployDomain(domainArchiveUri);
    }
  }

  /**
   * Deploys a domain bundled as a zip from the given URL to the mule container
   *
   * @param domainArchiveUri     location of the zip domain file.
   * @param deploymentProperties the properties to override during the deployment process.
   * @throws IOException
   */
  public static void deployDomain(DeploymentService delegate, URI domainArchiveUri, Properties deploymentProperties)
      throws IOException {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.deployDomain(domainArchiveUri, deploymentProperties);
    }
  }

  /**
   * Undeploys and redeploys a domain
   *
   * @param domainName           then name of the domain to redeploy.
   * @param deploymentProperties the properties to override during the deployment process.
   */
  public static void redeployDomain(DeploymentService delegate, String domainName, Properties deploymentProperties) {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.redeployDomain(domainName, deploymentProperties);
    }
  }

  /**
   * Undeploys and redeploys a domain
   *
   * @param domainName then name of the domain to redeploy
   */
  public static void redeployDomain(DeploymentService delegate, String domainName) {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.redeployDomain(domainName);
    }
  }

  /**
   * Deploys a domain bundle from the given URL to the mule container
   *
   * @param domainArchiveUri location of the ZIP domain file
   * @throws IOException if there is any problem reading the file
   */
  public static void deployDomainBundle(DeploymentService delegate, URI domainArchiveUri) throws IOException {
    try (DeploymentServiceLock lock = new DeploymentServiceLock(delegate)) {
      delegate.deployDomainBundle(domainArchiveUri);
    }
  }

  /**
   * Auxiliary auto-closeable class to be used in a try-with-resources scope. During this object's lifetime, the
   * DeploymentService's lock will be taken.
   */
  private static class DeploymentServiceLock implements AutoCloseable {

    private final ReentrantLock lock;

    DeploymentServiceLock(DeploymentService deploymentService) {
      this.lock = deploymentService.getLock();
      this.lock.lock();
    }

    @Override
    public void close() {
      lock.unlock();
    }
  }
}
