/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.metadata.java.JavaTypeLoader.JAVA;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestNoConfigMetadataResolver implements MetadataKeysResolver, MetadataOutputResolver, MetadataContentResolver
{

    public List<MetadataKey> getMetadataKeys(MetadataContext context)
    {
        return Arrays.stream(KeyIds.values())
                .map(e -> MetadataKeyBuilder.newKey(e.name()).build())
                .collect(Collectors.toList());
    }

    public MetadataType getContentMetadata(MetadataContext context, MetadataKey key)
    {
        if (key instanceof NullMetadataKey)
        {
            return BaseTypeBuilder.create(JAVA).nullType().build();
        }

        return BaseTypeBuilder.create(JAVA).stringType().build();
    }

    public MetadataType getOutputMetadata(MetadataContext context, MetadataKey key)
    {
        if (key instanceof NullMetadataKey)
        {
            return BaseTypeBuilder.create(JAVA).nullType().build();
        }

        return BaseTypeBuilder.create(JAVA).booleanType().build();
    }

    public enum KeyIds
    {
        BOOLEAN, STRING
    }
}
