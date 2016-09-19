/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.api;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages deploy of mule applications
 */
public interface DeploymentService extends DeploymentListenerManager, DomainDeploymentListenerManager {

  /**
   * Finds an active application by name.
   *
   * @return null if not found
   */
  Application findApplication(String appName);

  /**
   * Lists all applications that are deployed, starting or failed to start
   *
   * @return immutable applications list
   */
  List<Application> getApplications();

  /**
   * .
   * 
   * @param domainName name of the domain
   * @return the domain with the name domainName, null if there is no domain with domainName
   */
  Domain findDomain(String domainName);

  /**
   * @param domain name of a deployed domain
   * @return all the applications deployed in that domain
   */
  Collection<Application> findDomainApplications(final String domain);

  /**
   * @return list of domains deployed in mule.
   */
  List<Domain> getDomains();

  void addStartupListener(StartupListener listener);

  void removeStartupListener(StartupListener listener);

  /**
   * Obtains the object used to synchronize the service.
   *
   * @return a non null lock object.
   */
  ReentrantLock getLock();

  /**
   * Undeploys an application from the mule container
   *
   * @param appName name of the application to undeploy
   */
  void undeploy(String appName);

  /**
   * Deploys and application bundled as a zip from the given URL to the mule container
   *
   * @param appArchiveUrl location of the zip application file
   * @throws IOException
   */
  void deploy(URL appArchiveUrl) throws IOException;

  /**
   * Undeploys and redeploys an application
   *
   * @param artifactName then name of the application to redeploy
   */
  void redeploy(String artifactName);

  /**
   * Undeploys a domain from the mule container
   *
   * @param domainName name of the domain to undeploy
   */
  void undeployDomain(String domainName);

  /**
   * Deploys a domain bundled as a zip from the given URL to the mule container
   *
   * @param domainArchiveUrl location of the zip domain file
   * @throws IOException
   */
  void deployDomain(URL domainArchiveUrl) throws IOException;

  /**
   * Undeploys and redeploys a domain
   *
   * @param domainName then name of the domain to redeploy
   */
  void redeployDomain(String domainName);

  void start();

  void stop();
}
