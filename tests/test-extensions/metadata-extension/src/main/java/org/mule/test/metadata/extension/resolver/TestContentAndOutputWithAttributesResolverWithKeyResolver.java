/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataAttributesResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;

import java.util.Set;

public class TestContentAndOutputWithAttributesResolverWithKeyResolver
        implements MetadataKeysResolver, MetadataContentResolver<String>, MetadataOutputResolver<String>, MetadataAttributesResolver<String>
{

    @Override
    public Set<MetadataKey> getMetadataKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException
    {
        return TestMetadataResolverUtils.getKeys(context);
    }

    @Override
    public MetadataType getContentMetadata(MetadataContext context, String key) throws MetadataResolvingException
    {
        return TestMetadataResolverUtils.getMetadata(key);
    }

    @Override
    public MetadataType getOutputMetadata(MetadataContext context, String key) throws MetadataResolvingException
    {
        return TestMetadataResolverUtils.getMetadata(key);
    }

    @Override
    public MetadataType getAttributesMetadata(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException
    {
        return TestMetadataResolverUtils.getMetadata(key);
    }
}
