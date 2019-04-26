/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.artifact.ShutdownListener;
import org.mule.module.launcher.domain.Domain;

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
     * Lists all applications that are deployed, starting or failed to start
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
     * Adds a shutdown listener to be triggered on deployment service shutdown
     * 
     * @param shutdown listener to be added
     *
     * @since 3.9.3
     */
    void addShutdownListener(ShutdownListener listener);
    
    /**
     * Removes a shutdown listener to be triggered on deployment service shutdown
     * 
     * @param shutdown listener to be removed
     * 
     * @since 3.9.3
     */
    void removeShutdownListener(ShutdownListener listener);
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
     * Undeploys and redeploys an application with the properties provided.
     *
     * @param artifactName then name of the application to redeploy
     * @param deploymentProperties the properties to override
     */
    void redeploy(String artifactName, Properties deploymentProperties);
    
    /**
     * Deploys an applications bundled as a zip from a given URL and sets the provided appProperties.
     *
     * @param appArchiveUrl
     * @param appProperties
     * @throws IOException
     */
    void deploy(URL appArchiveUrl, Properties appProperties) throws IOException;

    /**
     * Undeploys and redeploys an application
     *
     * @param artifactName then name of the application to redeploy
     * @throws IOException 
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
     * Deploys a domain bundled as a zip from the given URL to the mule container
     *
     * @param domainArchiveUrl location of the zip domain file
     * @param appProperties the properties to override
     * @throws IOException
     */
    void deployDomain(URL domainArchiveUrl, Properties appProperties) throws IOException;

    /**
     * Undeploys and redeploys a domain
     *
     * @param domainName then name of the domain to redeploy
     */
    void redeployDomain(String domainName);

    /**
     * Undeploys and redeploys a domain
     *
     * @param domainName then name of the domain to redeploy
     * @param deploymentProperties the properties to override
     */
    void redeployDomain(String domainName, Properties deploymentProperties);

    void start();

    void stop();
}
