/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.domain.Domain;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  Manages deploy of mule applications
 */
public interface DeploymentService extends DeploymentListenerManager, DomainDeploymentListenerManager
{

    /**
     * Finds an active application by name.
     *
     * @return null if not found
     */
    Application findApplication(String appName);

    /**
     * Lists all deployed applications.
     *
     * @return immutable applications list
     */
    List<Application> getApplications();

    /**
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

    void undeploy(String appName);

    void deploy(URL appArchiveUrl) throws IOException;

    void redeploy(String artifactName);

    void start();

    void stop();
}
