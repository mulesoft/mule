/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.config.spring.SpringXmlDomainConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.*;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.ClassUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

public abstract class DomainFunctionalTestCase extends AbstractMuleTestCase
{

    private final Map<String, MuleContext> muleContexts = new HashMap<String, MuleContext>();
    private final List<MuleContext> disposedContexts = new ArrayList<MuleContext>();
    private MuleContext domainContext;

    protected abstract String getDomainConfig();

    public synchronized void disposeMuleContext(final MuleContext muleContext)
    {
        disposedContexts.add(muleContext);
        muleContext.dispose();
        new PollingProber(10000,100).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return muleContext.isDisposed();
            }

            @Override
            public String describeFailure()
            {
                return "mule context timeout during dispose";
            }
        });
    }

    protected ConfigurationBuilder getDomainBuilder(String configResource) throws Exception
    {
        return new SpringXmlDomainConfigurationBuilder(configResource);
    }

    @Before
    public void setUpMuleContexts() throws Exception
    {
        createDomainContext();
        ApplicationConfig[] applicationConfigs = getConfigResources();
        for (ApplicationConfig applicationConfig : applicationConfigs)
        {
            MuleContext muleContext = createAppMuleContext(applicationConfig.applicationResources);
            muleContext.start();
            muleContexts.put(applicationConfig.applicationName, muleContext);
        }
    }

    private void createDomainContext() throws Exception
    {
        List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>(3);
        ConfigurationBuilder cfgBuilder = getDomainBuilder(getDomainConfig());
        builders.add(new DefaultsConfigurationBuilder());
        builders.add(cfgBuilder);
        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        domainContext = muleContextFactory.createMuleContext(builders, new DefaultMuleContextBuilder());
        domainContext.start();
    }

    @After
    public void disposeMuleContexts()
    {
        for (MuleContext muleContext : muleContexts.values())
        {
            try
            {
                disposeMuleContext(muleContext);
            }
            catch (Exception e)
            {
                //Nothing to do
            }
        }
        muleContexts.clear();
    }

    protected MuleContext createAppMuleContext(String[] configResource) throws Exception
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
        builders.add(getAppBuilder(configResource));
        DefaultMuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        configureMuleContext(contextBuilder);
        context = muleContextFactory.createMuleContext(builders, contextBuilder);
        return context;
    }

    protected ConfigurationBuilder getAppBuilder(String[] configResource) throws Exception
    {
        SpringXmlConfigurationBuilder springXmlConfigurationBuilder = new SpringXmlConfigurationBuilder(configResource);
        springXmlConfigurationBuilder.setDomainContext(domainContext);
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

    public abstract ApplicationConfig[] getConfigResources();

    public MuleContext getMuleContextForApp(String applicationName)
    {
        return muleContexts.get(applicationName);
    }

    public class ApplicationConfig
    {

        String applicationName;
        String[] applicationResources;

        public ApplicationConfig(String applicationName, String[] applicationResources)
        {
            this.applicationName = applicationName;
            this.applicationResources = applicationResources;
        }
    }

}
