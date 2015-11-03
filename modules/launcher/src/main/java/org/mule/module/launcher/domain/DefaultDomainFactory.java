/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.module.launcher.DeploymentListener;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DefaultDomainFactory implements DomainFactory
{

    private final DomainClassLoaderRepository domainClassLoaderRepository;
    private final Map<String, Domain> domains = new HashMap<String, Domain>();

    protected DeploymentListener deploymentListener;

    public DefaultDomainFactory(DomainClassLoaderRepository domainClassLoaderRepository)
    {
        this.domainClassLoaderRepository = domainClassLoaderRepository;
    }

    public void setDeploymentListener(DeploymentListener deploymentListener)
    {
        this.deploymentListener = deploymentListener;
    }

    @Override
    public Domain createDefaultDomain() throws IOException
    {
        return createArtifact(DEFAULT_DOMAIN_NAME);
    }

    @Override
    public Domain createArtifact(String artifactName) throws IOException
    {
        if (domains.containsKey(artifactName))
        {
            return domains.get(artifactName);
        }
        if (artifactName.contains(" "))
        {
            throw new IllegalArgumentException("Mule application name may not contain spaces: " + artifactName);
        }
        DefaultMuleDomain defaultMuleDomain = new DefaultMuleDomain(domainClassLoaderRepository, artifactName);
        defaultMuleDomain.setDeploymentListener(deploymentListener);
        DomainWrapper domainWrapper = new DomainWrapper(defaultMuleDomain, this);
        domains.put(artifactName, domainWrapper);
        return domainWrapper;
    }

    @Override
    public File getArtifactDir()
    {
        return MuleContainerBootstrapUtils.getMuleDomainsDir();
    }

    public void dispose(DomainWrapper domain)
    {
        domains.remove(domain.getArtifactName());
    }

    public void start(DomainWrapper domainWrapper)
    {
        domains.put(domainWrapper.getArtifactName(), domainWrapper);
    }
}
