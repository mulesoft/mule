/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application.builder;

import org.mule.module.launcher.descriptor.DomainDescriptor;
import org.mule.module.launcher.domain.DefaultMuleDomain;
import org.mule.module.launcher.domain.DomainClassLoaderRepository;

import java.io.IOException;
import java.util.Properties;

/**
 * Builder for {@link DefaultMuleDomain}
 *
 */
public class DefaultMuleDomainBuilder implements MuleDomainBuilder<DefaultMuleDomain>
{
    private DomainClassLoaderRepository domainClassLoaderRepository;
    
    private DomainDescriptor descriptor;
    
    private Properties deploymentProperties;
    
    @Override
    public DefaultMuleDomain buildDomain() throws IOException
    {
        DefaultMuleDomain domain = new DefaultMuleDomain(domainClassLoaderRepository, descriptor);
        domain.setDeploymentProperties(deploymentProperties);
        return domain;
    }

    public void setDescriptor(DomainDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    public void setDomainClassLoaderRepository(DomainClassLoaderRepository domainClassLoaderRepository)
    {
        this.domainClassLoaderRepository = domainClassLoaderRepository;
    }

    public void setDeploymentProperties(Properties deploymentProperties)
    {
        this.deploymentProperties = deploymentProperties;
    }

}
