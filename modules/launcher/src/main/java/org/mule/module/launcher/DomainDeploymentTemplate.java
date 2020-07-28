/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import com.google.common.base.Optional;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.ApplicationStatus;
import org.mule.module.launcher.artifact.Artifact;
import org.mule.module.launcher.domain.Domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Optional.of;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static org.mule.module.launcher.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import static org.mule.module.launcher.application.ApplicationStatus.DEPLOYMENT_FAILED;
import static org.mule.module.launcher.application.ApplicationStatus.STARTED;

/**
 * Utility to hook callbacks just before and after a domain zip is redeployed in Mule.
 */
public final class DomainDeploymentTemplate implements ArtifactDeploymentTemplate
{
    private Collection<Application> domainApplications = Collections.emptyList();
    private final DefaultArchiveDeployer<Application> applicationDeployer;
    private final DeploymentService deploymentservice;

    public DomainDeploymentTemplate(DefaultArchiveDeployer<Application> applicationDeployer, DeploymentService deploymentservice)
    {
        this.applicationDeployer = applicationDeployer;
        this.deploymentservice = deploymentservice;
    }

    /**
     * Undeploys all applications that use this domain.
     */
    @Override
    public void preRedeploy(Artifact domain)
    {
        if (domain instanceof Domain)
        {
            domainApplications = deploymentservice.findDomainApplications(domain.getArtifactName());
            for (Application domainApplication : domainApplications)
            {
                applicationDeployer.undeployArtifactWithoutUninstall(domainApplication);
            }
        }
    }

    /**
     * Deploys applications that were undeployed when {@link #preRedeploy(Artifact)} was called..
     */
    @Override
    public void postRedeploy(Artifact domain)
    {
        postRedeploy(domain, Collections.<Application, ApplicationStatus>emptyMap());
    }

    /**
     * Deploys applications that were undeployed when {@link #preRedeploy(Artifact)} was called..
     */
    @Override
    public void postRedeploy(Artifact domain, Map<Application, ApplicationStatus> appStatusPreRedeployment)
    {
        if (domain != null && !domainApplications.isEmpty())
        {
            for (Application domainApplication : domainApplications)
            {
                applicationDeployer.preTrackArtifact(domainApplication);
                if (applicationDeployer.isUpdatedZombieArtifact(domainApplication.getArtifactName()))
                {
                    Optional<Properties> property = addShouldStartProperty(appStatusPreRedeployment.getOrDefault(domainApplication,STARTED));
                    applicationDeployer.deployExplodedArtifact(domainApplication.getArtifactName(), property);
                }
            }
        }
        domainApplications = Collections.emptyList();
    }

    private Optional<Properties> addShouldStartProperty(ApplicationStatus applicationStatus)
    {
        Properties newProperties = new Properties();
        boolean startArtifact = applicationStatus.equals(STARTED) || applicationStatus.equals(DEPLOYMENT_FAILED);
        newProperties.setProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, valueOf(startArtifact));
        return of(newProperties);
    }
}
