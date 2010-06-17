package org.mule.module.launcher;

import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.util.StringUtils;

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
        configuration.setId(desc.getAppName());
        final String encoding = desc.getEncoding();
        if (StringUtils.isNotBlank(encoding))
        {
            configuration.setDefaultEncoding(encoding);
        }
        return configuration;
    }

}
