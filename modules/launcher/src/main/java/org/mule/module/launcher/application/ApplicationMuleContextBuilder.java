/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static org.mule.config.bootstrap.ArtifactType.APP;

import org.mule.DefaultMuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.PropertiesMuleConfigurationFactory;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.util.StringUtils;
import org.mule.work.MuleWorkManager;

/**
 * Takes Mule application descriptor into account when building the context.
 */
public class ApplicationMuleContextBuilder extends DefaultMuleContextBuilder
{
    protected ApplicationDescriptor desc;

    public ApplicationMuleContextBuilder(ApplicationDescriptor desc)
    {
        this.desc = desc;
    }

    @Override
    protected DefaultMuleContext createDefaultMuleContext()
    {
        DefaultMuleContext muleContext = super.createDefaultMuleContext();
        muleContext.setArtifactType(APP);
        return muleContext;
    }

    @Override
    protected DefaultMuleConfiguration createMuleConfiguration()
    {
        final DefaultMuleConfiguration configuration = new DefaultMuleConfiguration(true);
        PropertiesMuleConfigurationFactory.initializeFromProperties(configuration, desc.getAppProperties());
        configuration.setId(desc.getName());
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
        final String threadName = String.format("[%s].Mule", desc.getName());
        return new MuleWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, threadName, getMuleConfiguration().getShutdownTimeout());

    }
}
