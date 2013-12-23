/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.module.launcher.AppBloodhound;
import org.mule.module.launcher.DefaultAppBloodhound;
import org.mule.module.launcher.DeploymentListener;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.DomainFactory;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

/**
 * Creates default mule applications
 */
public class DefaultApplicationFactory implements ApplicationFactory
{

    private final ApplicationClassLoaderFactory applicationClassLoaderFactory;
    private final DomainFactory domainFactory;
    protected DeploymentListener deploymentListener;

    public DefaultApplicationFactory(ApplicationClassLoaderFactory applicationClassLoaderFactory, DomainFactory domainFactory)
    {
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.domainFactory = domainFactory;
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

        AppBloodhound bh = new DefaultAppBloodhound();
        final ApplicationDescriptor descriptor = bh.fetch(appName);

        return createAppFrom(descriptor);
    }

    @Override
    public File getArtifactDir()
    {
        return MuleContainerBootstrapUtils.getMuleAppsDir();
    }

    protected Application createAppFrom(ApplicationDescriptor descriptor) throws IOException
    {
        DefaultMuleApplication delegate;
        if (StringUtils.isEmpty(descriptor.getDomain()))
        {
            delegate = new DefaultMuleApplication(descriptor, applicationClassLoaderFactory, domainFactory.createDefaultDomain());
        }
        else
        {
            delegate = new DefaultMuleApplication(descriptor, applicationClassLoaderFactory, domainFactory.createArtifact(descriptor.getDomain()));
        }

        if (deploymentListener != null)
        {
            delegate.setDeploymentListener(deploymentListener);
        }
        return new ApplicationWrapper(delegate);
    }

}
