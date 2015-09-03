/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.module.extension.internal.DefaultDescribingContext.CAPABILITY_EXTRACTORS;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.introspection.declaration.DescribingContext;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.spi.CapabilityExtractor;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Creates instances of {@link DescribingContext} holding an empty {@link DeclarationDescriptor} ready to
 * be used.
 * <p/>
 * To optimize the application's startup time, this factory performs SPI for the available
 * {@link CapabilityExtractor} implementations and caches those so that they can be propagated
 * on every {@link DescribingContext} created. The immutable {@link List} with those extractors
 * are added as a parameter with the key {@link DefaultDescribingContext#CAPABILITY_EXTRACTORS}.
 * <p/>
 * Instances of this class are reusable and thread-safe.
 *
 * @since 4.0
 */
public final class DescribingContextFactory
{

    private final List<CapabilityExtractor> capabilityExtractors;

    /**
     * Creates a new instance and performs a SPI lookup for the available {@link CapabilityExtractor} implementations
     *
     * @param serviceRegistry the registry to use to perform the lookup
     * @param classLoader     the {@link ClassLoader} to perform the lookup on.
     */
    public DescribingContextFactory(ServiceRegistry serviceRegistry, ClassLoader classLoader)
    {
        capabilityExtractors = ImmutableList.copyOf(serviceRegistry.lookupProviders(CapabilityExtractor.class, classLoader));
    }

    /**
     * Creates a new {@link DescribingContext} as described in the class javadoc
     *
     * @return a new {@link DescribingContext}
     */
    public DescribingContext newDescribingContext()
    {
        DescribingContext context = new DefaultDescribingContext(new DeclarationDescriptor());
        context.addParameter(CAPABILITY_EXTRACTORS, capabilityExtractors);

        return context;
    }
}
