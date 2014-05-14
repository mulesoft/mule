/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extensions.introspection.Describer;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.ExtensionFactory;
import org.mule.module.extensions.internal.introspection.ExtensionDiscoverer;

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
    public List<Extension> discover(ClassLoader classLoader)
    {
        checkArgument(classLoader != null, "classloader cannot be null");

        Collection<Describer> describers = serviceRegistry.lookupProviders(Describer.class, classLoader);
        if (describers.isEmpty()) {
            return ImmutableList.of();
        }

        ImmutableList.Builder<Extension> builder = ImmutableList.builder();
        for (Describer describer : describers)
        {
            builder.add(extensionFactory.createFrom(describer.describe()));
        }

        return builder.build();
    }
}
