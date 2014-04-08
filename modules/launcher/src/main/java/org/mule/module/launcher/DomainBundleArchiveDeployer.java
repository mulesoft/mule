/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.launcher.artifact.ArtifactFactory;
import org.mule.module.launcher.domain.Domain;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DomainBundleArchiveDeployer implements ArchiveDeployer<Domain>
{

    private transient final Log logger = LogFactory.getLog(getClass());

    public static final String DOMAIN_BUNDLE_APPS_FOLDER = "apps";
    private final ArchiveDeployer<Domain> domainDeployer;

    public DomainBundleArchiveDeployer(ArchiveDeployer<Domain> domainDeployer)
    {
        this.domainDeployer = domainDeployer;
    }

    @Override
    public Domain deployPackagedArtifact(String zip) throws DeploymentException
    {
        Domain domain = domainDeployer.deployPackagedArtifact(zip);
        deployBundledAppsIfDomainWasCreated(domain);
        return domain;
    }

    @Override
    public Domain deployExplodedArtifact(String artifactDir) throws DeploymentException
    {
        Domain domain = domainDeployer.deployExplodedArtifact(artifactDir);
        deployBundledAppsIfDomainWasCreated(domain);
        return domain;
    }

    @Override
    public Domain deployPackagedArtifact(URL artifactAchivedUrl)
    {
        Domain domain = domainDeployer.deployPackagedArtifact(artifactAchivedUrl);
        deployBundledAppsIfDomainWasCreated(domain);
        return domain;
    }

    @Override
    public void undeployArtifact(String artifactDir)
    {
        domainDeployer.undeployArtifact(artifactDir);
    }

    @Override
    public File getDeploymentDirectory()
    {
        return domainDeployer.getDeploymentDirectory();
    }

    @Override
    public void setDeploymentListener(CompositeDeploymentListener deploymentListener)
    {
        domainDeployer.setDeploymentListener(deploymentListener);
    }

    @Override
    public void redeploy(Domain artifact)
    {
        domainDeployer.redeploy(artifact);
    }

    @Override
    public Map<URL, Long> getArtifactsZombieMap()
    {
        return domainDeployer.getArtifactsZombieMap();
    }

    @Override
    public void setArtifactFactory(ArtifactFactory<Domain> artifactFactory)
    {
        domainDeployer.setArtifactFactory(artifactFactory);
    }

    private void deployBundledAppsIfDomainWasCreated(Domain domain)
    {
        if (domain != null)
        {
            deployBundleApps(domain);
        }
    }

    private void deployBundleApps(Domain domain)
    {
        File domainFolder = new File(domainDeployer.getDeploymentDirectory(), domain.getArtifactName());
        File appsFolder = new File(domainFolder, DOMAIN_BUNDLE_APPS_FOLDER);
        if (appsFolder.exists())
        {
            File[] files = appsFolder.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.endsWith(".zip");
                }
            });
            for (File file : files)
            {
                try
                {
                    FileUtils.moveFile(file, new File(MuleContainerBootstrapUtils.getMuleAppsDir(), file.getName()));
                }
                catch (IOException e)
                {
                    logger.warn(e.getMessage());
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(e);
                    }
                }
            }
        }
    }
}
