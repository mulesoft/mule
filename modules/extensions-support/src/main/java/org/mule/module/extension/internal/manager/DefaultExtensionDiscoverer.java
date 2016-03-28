/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.api.introspection.ExtensionDiscoverer;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.extension.api.introspection.declaration.spi.Describer;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.registry.SpiServiceRegistry;
import org.mule.util.collection.ImmutableListCollector;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * Default implementation of {@link ExtensionDiscoverer}
 *
 * @since 4.0.0
 */
public final class DefaultExtensionDiscoverer implements ExtensionDiscoverer
{

    private final ExtensionFactory extensionFactory;
    private final ServiceRegistry serviceRegistry;

    public DefaultExtensionDiscoverer()
    {
        this.serviceRegistry = new SpiServiceRegistry();
        this.extensionFactory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());
    }

    public DefaultExtensionDiscoverer(ExtensionFactory extensionFactory, ServiceRegistry serviceRegistry)
    {
        this.extensionFactory = extensionFactory;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RuntimeExtensionModel> discover(ClassLoader classLoader)
    {
        checkArgument(classLoader != null, "classloader cannot be null");

        Collection<Describer> describers = serviceRegistry.lookupProviders(Describer.class, classLoader);
        if (describers.isEmpty())
        {
            return ImmutableList.of();
        }

        return describers.stream().map(describer -> {
            ExtensionDeclarer declarer = describer.describe(new DefaultDescribingContext());
            return extensionFactory.createFrom(declarer);
        }).collect(new ImmutableListCollector<>());
    }
}
