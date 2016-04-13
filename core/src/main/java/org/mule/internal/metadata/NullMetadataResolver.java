/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.metadata;

import org.mule.api.metadata.MetadataContext;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataResolvingException;
import org.mule.api.metadata.resolving.MetadataContentResolver;
import org.mule.api.metadata.resolving.MetadataKeysResolver;
import org.mule.api.metadata.resolving.MetadataOutputResolver;
import org.mule.extension.api.annotation.metadata.Content;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.java.JavaTypeLoader;

import java.util.Collections;
import java.util.List;

/**
 * Null implementation of {@link MetadataContentResolver}, {@link MetadataOutputResolver} and {@link MetadataKeysResolver},
 * used to represent the absence of any of them when required.
 *
 * @since 1.0
 */
public final class NullMetadataResolver implements MetadataContentResolver, MetadataOutputResolver, MetadataKeysResolver
{

    /**
     * Null implementation of {@link MetadataKeysResolver}, used when no implementation
     * is provided by the connector developer. Represents the absence of a custom {@link MetadataKeysResolver},
     * returning an empty list of {@link MetadataKey}.
     *
     * @param context MetaDataContext of the MetaData resolution
     * @return {@link Collections#emptyList()}
     * @throws MetadataResolvingException
     */
    public List<MetadataKey> getMetadataKeys(MetadataContext context) throws MetadataResolvingException
    {
        return Collections.emptyList();
    }

    /**
     * Null implementation of {@link MetadataContentResolver}, used when no implementation
     * is provided by the connector developer. Represents the absence of a custom {@link MetadataContentResolver},
     * returning a {@link NullType} instead of resolving a valid {@link MetadataType} for the {@link Content} parameter
     *
     * @param context MetaDataContext of the MetaData resolution
     * @param key     {@link MetadataKey} of the type which's structure has to be resolved
     * @return {@link NullType}
     * @throws MetadataResolvingException
     */
    public MetadataType getContentMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException
    {
        return BaseTypeBuilder.create(JavaTypeLoader.JAVA).nullType().build();
    }

    /**
     * Null implementation of {@link MetadataOutputResolver}, used when no implementation
     * is provided by the connector developer. Represents the absence of a custom {@link MetadataOutputResolver},
     * returning a {@link NullType} instead of resolving a dynamic {@link MetadataType} for the component's output.
     *
     * @param context MetaDataContext of the MetaData resolution
     * @param key     {@link MetadataKey} of the type which's structure has to be resolved
     * @return {@link NullType}
     * @throws MetadataResolvingException
     */
    public MetadataType getOutputMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException
    {
        return BaseTypeBuilder.create(JavaTypeLoader.JAVA).nullType().build();
    }

}
