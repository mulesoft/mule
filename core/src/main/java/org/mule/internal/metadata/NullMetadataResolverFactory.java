/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.metadata;

import org.mule.api.metadata.resolving.MetadataContentResolver;
import org.mule.api.metadata.resolving.MetadataKeysResolver;
import org.mule.api.metadata.resolving.MetadataOutputResolver;
import org.mule.extension.api.introspection.metadata.MetadataResolverFactory;

/**
 * Null implementation of a {@link MetadataResolverFactory}, which returns
 * a {@link NullMetadataResolver} for every resolver provided by the factory
 *
 * @since 4.0
 */
public class NullMetadataResolverFactory implements MetadataResolverFactory
{

    private final NullMetadataResolver metadataResolver;

    public NullMetadataResolverFactory()
    {
        this.metadataResolver = new NullMetadataResolver();
    }

    /**
     * @return a {@link NullMetadataResolver} implementation of {@link MetadataKeysResolver}
     */
    @Override
    public MetadataKeysResolver getKeyResolver()
    {
        return metadataResolver;
    }

    /**
     * @return a {@link NullMetadataResolver} implementation of {@link MetadataContentResolver}
     */
    @Override
    public MetadataContentResolver getContentResolver()
    {
        return metadataResolver;
    }

    /**
     * @return a {@link NullMetadataResolver} implementation of {@link MetadataOutputResolver}
     */
    @Override
    public MetadataOutputResolver getOutputResolver()
    {
        return metadataResolver;
    }
}
