/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;

import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.api.context.MuleContextFactory;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.TestingWorkListener;

import java.util.ArrayList;
import java.util.List;

public class ApplicationContextBuilder
{

    private MuleContext domainContext;
    private String[] applicationResources;
    private MuleContextBuilder muleContextBuilder = new DefaultMuleContextBuilder()
    {
        @Override
        protected DefaultMuleContext createDefaultMuleContext()
        {
            DefaultMuleContext muleContext = super.createDefaultMuleContext();
            muleContext.setArtifactType(APP);
            return muleContext;
        }
    };

    public ApplicationContextBuilder setDomainContext(MuleContext domainContext)
    {
        this.domainContext = domainContext;
        return this;
    }

    public ApplicationContextBuilder setApplicationResources(String[] applicationResources)
    {
        this.applicationResources = applicationResources;
        return this;
    }

    public MuleContext build() throws Exception
    {
        // Should we set up the manager for every method?
        MuleContext context = doBuildContext();
        context.start();
        return context;
    }

    protected MuleContext doBuildContext() throws Exception
    {
        MuleContext context;
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        builders.add(getAppBuilder(this.applicationResources));
        configureMuleContext(muleContextBuilder);
        context = muleContextFactory.createMuleContext(builders, muleContextBuilder);
        return context;
    }

    protected ConfigurationBuilder getAppBuilder(String[] configResource) throws Exception
    {
        SpringXmlConfigurationBuilder springXmlConfigurationBuilder = new SpringXmlConfigurationBuilder(configResource);
        if (domainContext != null)
        {
            springXmlConfigurationBuilder.setParentContext(domainContext);
        }
        return springXmlConfigurationBuilder;
    }

    /**
     * Override this method to set properties of the MuleContextBuilder before it is
     * used to create the MuleContext.
     */
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        contextBuilder.setWorkListener(new TestingWorkListener());
    }
}
