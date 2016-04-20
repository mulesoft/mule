/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.runtime.api.connection .ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.metadata.api.model.MetadataType;

public class TestContentResolverWithoutKeyResolver implements MetadataContentResolver
{

    @Override
    public MetadataType getContentMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException, ConnectionException
    {
        return TestMetadataResolverUtils.getMetadata(TestMetadataResolverUtils.getKeys(context).get(0));
    }
}
