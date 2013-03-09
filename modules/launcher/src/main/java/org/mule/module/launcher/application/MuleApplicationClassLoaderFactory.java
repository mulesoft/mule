/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import org.mule.module.launcher.DefaultMuleSharedDomainClassLoader;
import org.mule.module.launcher.MuleApplicationClassLoader;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.plugin.MulePluginsClassLoader;
import org.mule.module.launcher.plugin.PluginDescriptor;
import org.mule.util.StringUtils;

import java.util.Set;

/**
 * Creates {@link MuleApplicationClassLoader} instances based on the
 * application descriptor.
 */
public class MuleApplicationClassLoaderFactory implements ApplicationClassLoaderFactory
{

    @Override
    public ClassLoader create(ApplicationDescriptor descriptor)
    {
        final String domain = descriptor.getDomain();
        ClassLoader parent;

        if (StringUtils.isBlank(domain) || DefaultMuleSharedDomainClassLoader.DEFAULT_DOMAIN_NAME.equals(domain))
        {
            parent = new DefaultMuleSharedDomainClassLoader(getClass().getClassLoader());
        }
        else
        {
            // TODO handle non-existing domains with an exception
            parent = new MuleSharedDomainClassLoader(domain, getClass().getClassLoader());
        }

        final Set<PluginDescriptor> plugins = descriptor.getPlugins();
        if (!plugins.isEmpty())
        {
            // re-assign parent ref if any plugins deployed, will be used by the MuleAppCL
            parent = new MulePluginsClassLoader(parent, plugins);
        }

        return new MuleApplicationClassLoader(descriptor.getAppName(), parent, descriptor.getLoaderOverride());
    }
}
