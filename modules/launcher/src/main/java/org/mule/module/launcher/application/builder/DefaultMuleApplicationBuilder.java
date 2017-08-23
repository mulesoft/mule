/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application.builder;

import static org.mule.module.launcher.DeploymentPropertiesUtils.resolveDeploymentProperties;

import org.mule.module.launcher.application.ApplicationClassLoaderFactory;
import org.mule.module.launcher.application.DefaultMuleApplication;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.domain.Domain;

import java.io.IOException;
import java.util.Properties;

/**
 * Builder for {@link DefaultMuleApplication}
 */
public class DefaultMuleApplicationBuilder implements MuleApplicationBuilder<DefaultMuleApplication>
{

    private ApplicationDescriptor descriptor;

    private ApplicationClassLoaderFactory applicationClassLoaderFactory;

    private Properties deploymentProperties;

    private Domain domain;

    @Override
    public DefaultMuleApplication buildApplication() throws IOException
    {
        DefaultMuleApplication application = new DefaultMuleApplication(descriptor, applicationClassLoaderFactory, domain);
        application.setDeploymentProperties(deploymentProperties);
        return application;
    }

    public void setDescriptor(ApplicationDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    public void setApplicationClassLoaderFactory(ApplicationClassLoaderFactory applicationClassLoaderFactory)
    {
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
    }

    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    public void setDeploymentProperties(Properties deploymentProperties)
    {
        this.deploymentProperties = deploymentProperties;
    }

}
