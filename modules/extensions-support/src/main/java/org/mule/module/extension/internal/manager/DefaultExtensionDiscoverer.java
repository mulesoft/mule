/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.spi.Describer;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.internal.introspection.ExtensionDiscoverer;
import org.mule.util.collection.ImmutableListCollector;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * Default implementation of {@link ExtensionDiscoverer}
 *
 * @since 3.7.0
 */
final class DefaultExtensionDiscoverer implements ExtensionDiscoverer
{

    private final ExtensionFactory extensionFactory;
    private final ServiceRegistry serviceRegistry;

    public DefaultExtensionDiscoverer(ExtensionFactory extensionFactory, ServiceRegistry serviceRegistry)
    {
        this.extensionFactory = extensionFactory;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExtensionModel> discover(ClassLoader classLoader)
    {
        checkArgument(classLoader != null, "classloader cannot be null");

        Collection<Describer> describers = serviceRegistry.lookupProviders(Describer.class, classLoader);
        if (describers.isEmpty())
        {
            return ImmutableList.of();
        }

        return describers.stream().map(describer -> {
            Descriptor descriptor = describer.describe(new DefaultDescribingContext());
            return extensionFactory.createFrom(descriptor);
        }).collect(new ImmutableListCollector<>());
    }
}
