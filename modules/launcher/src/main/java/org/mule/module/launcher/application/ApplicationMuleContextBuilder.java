/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.PropertiesMuleConfigurationFactory;
import org.mule.module.launcher.artifact.ArtifactMuleContextBuilder;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.util.StringUtils;
import org.mule.work.MuleWorkManager;

/**
 * Takes Mule application descriptor into account when building the context.
 */
public class ApplicationMuleContextBuilder extends ArtifactMuleContextBuilder
{
    protected ApplicationDescriptor desc;

    public ApplicationMuleContextBuilder(ApplicationDescriptor desc)
    {
        this.desc = desc;
    }

    @Override
    protected void configureClassLoaderMuleContext(MuleContext muleContext)
    {
        ((ApplicationClassLoader) Thread.currentThread().getContextClassLoader()).setMuleContext(muleContext);
    }

    @Override
    public MuleContext buildMuleContext()
    {
        return super.buildMuleContext();
    }

    @Override
    protected DefaultMuleConfiguration createMuleConfiguration()
    {
        final DefaultMuleConfiguration configuration = new DefaultMuleConfiguration(true);
        PropertiesMuleConfigurationFactory.initializeFromProperties(configuration, desc.getAppProperties());
        configuration.setId(desc.getAppName());
        final String encoding = desc.getEncoding();
        if (StringUtils.isNotBlank(encoding))
        {
            configuration.setDefaultEncoding(encoding);
        }
        return configuration;
    }

    @Override
    protected MuleWorkManager createWorkManager()
    {
        // use app name in the core Mule thread
        final String threadName = String.format("[%s].Mule", desc.getAppName());
        return new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, threadName, getMuleConfiguration().getShutdownTimeout());

    }
}
