/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.DOMAIN;

import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;

import java.util.ArrayList;
import java.util.List;

public class DomainContextBuilder
{

    private String domainConfig;
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
        domainContext.start();
        return domainContext;
    }

    protected ConfigurationBuilder getDomainBuilder(String configResource) throws Exception
    {
        return new SpringXmlConfigurationBuilder(configResource, emptyMap(), DOMAIN);
    }
}
