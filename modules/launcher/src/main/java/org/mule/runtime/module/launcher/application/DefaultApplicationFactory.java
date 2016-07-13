/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.module.launcher.ApplicationDescriptorFactory;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.artifact.ArtifactFactory;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;

/**
 * Creates default mule applications
 */
public class DefaultApplicationFactory implements ArtifactFactory<Application>
{

    private final ApplicationDescriptorFactory applicationDescriptorFactory;
    private final DomainRepository domainRepository;
    private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory;
    protected DeploymentListener deploymentListener;

    public DefaultApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory, ApplicationDescriptorFactory applicationDescriptorFactory, DomainRepository domainRepository)
    {
        checkArgument(applicationClassLoaderBuilderFactory != null, "Application classloader builder factory cannot be null");
        checkArgument(applicationDescriptorFactory != null, "Application descriptor factory cannot be null");
        checkArgument(domainRepository != null, "Domain repository cannot be null");

        this.applicationClassLoaderBuilderFactory = applicationClassLoaderBuilderFactory;
        this.applicationDescriptorFactory = applicationDescriptorFactory;
        this.domainRepository = domainRepository;
    }

    public void setDeploymentListener(DeploymentListener deploymentListener)
    {
        this.deploymentListener = deploymentListener;
    }

    public Application createArtifact(String appName) throws IOException
    {
        if (appName.contains(" "))
        {
            throw new IllegalArgumentException("Mule application name may not contain spaces: " + appName);
        }

        final File appsDir = MuleContainerBootstrapUtils.getMuleAppsDir();
        final ApplicationDescriptor descriptor = applicationDescriptorFactory.create(new File(appsDir, appName));

        return createAppFrom(descriptor);
    }

    @Override
    public File getArtifactDir()
    {
        return MuleContainerBootstrapUtils.getMuleAppsDir();
    }

    protected Application createAppFrom(ApplicationDescriptor descriptor) throws IOException
    {
        ArtifactClassLoader applicationClassLoader = applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()
                .setDomain(descriptor.getDomain())
                .setPluginsSharedLibFolder(descriptor.getSharedPluginFolder())
                .addArtifactPluginDescriptor(descriptor.getPlugins().toArray(new ArtifactPluginDescriptor[0]))
                .setArtifactId(descriptor.getName())
                .setArtifactDescriptor(descriptor)
                .build();

        DefaultMuleApplication delegate = new DefaultMuleApplication(descriptor, applicationClassLoader, domainRepository);

        if (deploymentListener != null)
        {
            delegate.setDeploymentListener(deploymentListener);
        }

        return new ApplicationWrapper(delegate);
    }

}
