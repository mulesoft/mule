/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.metadata.api.model.MetadataType;

import java.util.List;

public class TestMetadataResolverMetadataResolvingFailure implements MetadataKeysResolver, MetadataContentResolver, MetadataOutputResolver
{

    public List<MetadataKey> getMetadataKeys(MetadataContext context) throws MetadataResolvingException
    {
        throw new MetadataResolvingException("Failing keysResolver retriever", FailureCode.CONNECTION_FAILURE);
    }

    public MetadataType getContentMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException
    {
        throw new MetadataResolvingException("Failing keysResolver retriever", FailureCode.CONNECTION_FAILURE);
    }

    public MetadataType getOutputMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException
    {
        throw new MetadataResolvingException("Failing keysResolver retriever", FailureCode.CONNECTION_FAILURE);
    }

}
