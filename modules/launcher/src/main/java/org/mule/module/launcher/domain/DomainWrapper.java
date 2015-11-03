/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.artifact.ArtifactWrapper;

import java.io.IOException;

/**
 * Domain wrapper used to notify domain factory that a domain has been disposed or started.
 */
public class DomainWrapper extends ArtifactWrapper<Domain> implements Domain
{

    private final DefaultDomainFactory domainFactory;

    protected DomainWrapper(final Domain delegate, final DefaultDomainFactory domainFactory) throws IOException
    {
        super(delegate);
        this.domainFactory = domainFactory;
    }

    @Override
    public boolean containsSharedResources()
    {
        return getDelegate().containsSharedResources();
    }

    @Override
    public MuleContext getMuleContext()
    {
        return getDelegate().getMuleContext();
    }

    @Override
    public ConfigurationBuilder createApplicationConfigurationBuilder(Application application) throws Exception
    {
        return getDelegate().createApplicationConfigurationBuilder(application);
    }

    @Override
    public void dispose()
    {
        try
        {
            getDelegate().dispose();
        }
        finally
        {
            domainFactory.dispose(this);
        }
    }

    @Override
    public void start() throws DeploymentStartException
    {
        super.start();
        domainFactory.start(this);
    }

}
