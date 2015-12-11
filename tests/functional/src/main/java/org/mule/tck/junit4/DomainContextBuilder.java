/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static org.mule.config.bootstrap.ArtifactType.DOMAIN;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.config.spring.SpringXmlDomainConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;

import java.util.ArrayList;
import java.util.List;

public class DomainContextBuilder
{

    private String domainConfig;
    private boolean disableMuleContextStart = false;
    private MuleContextBuilder muleContextBuilder = new DefaultMuleContextBuilder()
    {
        @Override
        protected DefaultMuleContext createDefaultMuleContext()
        {
            DefaultMuleContext muleContext = super.createDefaultMuleContext();
            muleContext.setArtifactType(DOMAIN);
            return muleContext;
        }
    };

    public DomainContextBuilder setDomainConfig(String domainConfig)
    {
        this.domainConfig = domainConfig;
        return this;
    }

    public MuleContext build() throws Exception
    {
        List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>(3);
        ConfigurationBuilder cfgBuilder = getDomainBuilder(domainConfig);
        builders.add(cfgBuilder);
        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        MuleContext domainContext = muleContextFactory.createMuleContext(builders, muleContextBuilder);
        if (!disableMuleContextStart)
        {
            domainContext.start();
        }
        return domainContext;
    }

    protected ConfigurationBuilder getDomainBuilder(String configResource) throws Exception
    {
        return new SpringXmlDomainConfigurationBuilder(configResource);
    }

    public void disableMuleContextStart()
    {
        this.disableMuleContextStart = true;
    }

}
