/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata.extension.resolver;

import org.mule.api.metadata.MetadataContext;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataResolvingException;
import org.mule.api.metadata.resolving.MetadataContentResolver;
import org.mule.api.metadata.resolving.MetadataOutputResolver;
import org.mule.metadata.api.model.MetadataType;
import org.mule.module.extension.internal.metadata.TestMetadataUtils;

public class TestContentAndOutputResolverWithoutKeyResolver implements MetadataContentResolver, MetadataOutputResolver
{

    @Override
    public MetadataType getContentMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException
    {
        return TestMetadataUtils.getMetadata(key);
    }

    @Override
    public MetadataType getOutputMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException
    {
        return TestMetadataUtils.getMetadata(key);
    }
}
