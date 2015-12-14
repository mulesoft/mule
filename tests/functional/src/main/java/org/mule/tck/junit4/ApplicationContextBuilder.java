/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static org.mule.config.bootstrap.ArtifactType.APP;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.TestingWorkListener;
import org.mule.util.ClassUtils;

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
        MuleContext context;
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        //If the annotations module is on the classpath, add the annotations config builder to the list
        //This will enable annotations config for this instance
        if (ClassUtils.isClassOnPath(AbstractMuleContextTestCase.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
        {
            builders.add((ConfigurationBuilder) ClassUtils.instanciateClass(AbstractMuleContextTestCase.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER,
                                                                            ClassUtils.NO_ARGS, getClass()));
        }
        builders.add(getAppBuilder(this.applicationResources));
        configureMuleContext(muleContextBuilder);
        context = muleContextFactory.createMuleContext(builders, muleContextBuilder);
        context.start();
        return context;
    }

    protected ConfigurationBuilder getAppBuilder(String[] configResource) throws Exception
    {
        SpringXmlConfigurationBuilder springXmlConfigurationBuilder = new SpringXmlConfigurationBuilder(configResource);
        if (domainContext != null)
        {
            springXmlConfigurationBuilder.setDomainContext(domainContext);
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
