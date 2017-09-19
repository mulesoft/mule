/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static com.google.common.base.Optional.absent;
import static org.mule.module.launcher.DeploymentPropertiesUtils.resolveDeploymentProperties;
import static org.mule.module.launcher.MuleFoldersUtil.getDomainFolder;
import static org.mule.module.launcher.artifact.ArtifactFactoryUtils.getDeploymentFile;
import static org.mule.module.launcher.descriptor.ArtifactDescriptor.DEFAULT_DEPLOY_PROPERTIES_RESOURCE;

import org.mule.module.launcher.DeploymentListener;
import org.mule.module.launcher.application.builder.DefaultMuleDomainBuilder;
import org.mule.module.launcher.descriptor.DomainDescriptor;
import org.mule.module.launcher.descriptor.DomainDescriptorParser;
import org.mule.module.launcher.descriptor.EmptyDomainDescriptor;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Optional;

public class DefaultDomainFactory implements DomainFactory
{

    private final DomainClassLoaderRepository domainClassLoaderRepository;
    private final Map<String, Domain> domains = new HashMap<String, Domain>();
    private final DomainDescriptorParser domainDescriptorParser;

    protected DeploymentListener deploymentListener;

    public DefaultDomainFactory(DomainClassLoaderRepository domainClassLoaderRepository)
    {
        this.domainClassLoaderRepository = domainClassLoaderRepository;
        this.domainDescriptorParser = new DomainDescriptorParser();
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
    public Domain createArtifact(String artifactName, Optional<Properties> deploymentProperties) throws IOException
    {
        if (domains.containsKey(artifactName))
        {
            return domains.get(artifactName);
        }
        if (artifactName.contains(" "))
        {
            throw new IllegalArgumentException("Mule application name may not contain spaces: " + artifactName);
        }
        DomainDescriptor descriptor = findDomain(artifactName);
        DefaultMuleDomain defaultMuleDomain = createDefaultMuleDomain(domainClassLoaderRepository, descriptor, deploymentProperties);
        defaultMuleDomain.setDeploymentListener(deploymentListener);
        DomainWrapper domainWrapper = new DomainWrapper(defaultMuleDomain, this);
        domains.put(artifactName, domainWrapper);
        return domainWrapper;
    }

    private DefaultMuleDomain createDefaultMuleDomain(DomainClassLoaderRepository domainClassLoaderRepository, DomainDescriptor descriptor, Optional<Properties> deploymentProperties) throws IOException
    {
        DefaultMuleDomainBuilder builder = new DefaultMuleDomainBuilder();
        builder.setDescriptor(descriptor);
        builder.setDomainClassLoaderRepository(domainClassLoaderRepository);
        builder.setDeploymentProperties(resolveDeploymentProperties(descriptor.getName(), deploymentProperties));
        return builder.buildDomain();
    }

    private DomainDescriptor findDomain(String domainName) throws IOException
    {
        if (DEFAULT_DOMAIN_NAME.equals(domainName))
        {
            return new EmptyDomainDescriptor(DEFAULT_DOMAIN_NAME);
        }

        final File deploymentFile = getDeploymentFile(getDomainFolder(domainName), domainName, DEFAULT_DEPLOY_PROPERTIES_RESOURCE);

        DomainDescriptor descriptor;

        if (deploymentFile != null)
        {
            descriptor = domainDescriptorParser.parse(deploymentFile);
            descriptor.setName(domainName);
        }
        else
        {
            descriptor = new EmptyDomainDescriptor(domainName);
        }

        return descriptor;
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

    @Override
    public Domain createArtifact(String artifactName) throws IOException
    {
        Optional<Properties> properties = absent();
        return createArtifact(artifactName, properties);
    }
}
