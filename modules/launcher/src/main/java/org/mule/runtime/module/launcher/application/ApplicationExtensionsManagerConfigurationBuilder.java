/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.core.registry.SpiServiceRegistry;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of {@link org.mule.runtime.core.api.config.ConfigurationBuilder}
 * that register a {@link ExtensionManager} if it's present in the classpath
 *
 * @since 4.0
 */
public class ApplicationExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder
{

    private static Log logger = LogFactory.getLog(ApplicationExtensionsManagerConfigurationBuilder.class);

    private final List<ApplicationPlugin> applicationPlugins;

    public ApplicationExtensionsManagerConfigurationBuilder(List<ApplicationPlugin> applicationPlugins)
    {
        this.applicationPlugins = applicationPlugins;
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        ExtensionManager extensionManager = new DefaultExtensionManager();
        ((DefaultMuleContext) muleContext).setExtensionManager(extensionManager);
        initialiseIfNeeded(extensionManager, muleContext);

        for (ApplicationPlugin applicationPlugin : applicationPlugins)
        {
            final ServiceLoader<Describer> describers = ServiceLoader.load(Describer.class, new ClassLoader(null)
            {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException
                {
                    return applicationPlugin.getArtifactClassLoader().getClassLoader().loadClass(name);
                }

                @Override
                protected URL findResource(String name)
                {
                    return applicationPlugin.getArtifactClassLoader().findResource(name);
                }

                @Override
                protected Enumeration<URL> findResources(String name) throws IOException
                {
                    return applicationPlugin.getArtifactClassLoader().findResources(name);
                }
            });

            for (Describer describer : describers)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Discovered extension: " + describer.getClass().getName());
                }

                ExtensionDeclarer declarer = describer.describe(new DefaultDescribingContext());
                final DefaultExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), applicationPlugin.getArtifactClassLoader().getClassLoader());
                final RuntimeExtensionModel extensionModel = extensionFactory.createFrom(declarer);
                extensionManager.registerExtension(extensionModel);
            }
        }
    }
}
