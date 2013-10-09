/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

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
