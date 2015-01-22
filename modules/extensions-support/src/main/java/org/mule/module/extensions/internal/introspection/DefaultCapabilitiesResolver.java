/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extensions.introspection.declaration.Construct;
import org.mule.extensions.introspection.declaration.DeclarationConstruct;
import org.mule.extensions.introspection.declaration.HasCapabilities;
import org.mule.module.extensions.CapabilityExtractor;

import java.util.Collection;

/**
 * Default implementation of {@link CapabilitiesResolver} which
 * relies on {@link CapabilityExtractor} that are obtained through a
 * {@link ServiceRegistry}
 *
 * @since 3.7.0
 */
final class DefaultCapabilitiesResolver implements CapabilitiesResolver
{

    private final ServiceRegistry serviceRegistry;

    public DefaultCapabilitiesResolver(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resolveCapabilities(DeclarationConstruct declaration, Class<?> capableType, HasCapabilities<? extends Construct> capableCallback)
    {
        checkArgument(declaration != null, "declaration construct cannot be null");
        checkArgument(capableType != null, "capable type cannot be null");
        checkArgument(capableCallback != null, "capable callback cannot be null");

        for (CapabilityExtractor extractor : getExtractors())
        {
            extractor.extractCapability(declaration, capableType, capableCallback);
        }
    }

    private Collection<CapabilityExtractor> getExtractors()
    {
        return serviceRegistry.lookupProviders(CapabilityExtractor.class, getClass().getClassLoader());
    }

}
