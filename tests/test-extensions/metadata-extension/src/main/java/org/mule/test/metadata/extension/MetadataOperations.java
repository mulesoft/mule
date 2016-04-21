/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyParam;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;
import org.mule.test.metadata.extension.resolver.TestContentAndOutputResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestContentAndOutputResolverWithoutKeyResolver;
import org.mule.test.metadata.extension.resolver.TestContentResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestContentResolverWithoutKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputResolverWithoutKeyResolver;
import org.mule.test.metadata.extension.resolver.TestResolverWithCache;

@MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
        contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
        outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
public class MetadataOperations extends MetadataOperationsParent
{

    @MetadataScope(keysResolver = TestContentResolverWithKeyResolver.class, contentResolver = TestContentResolverWithKeyResolver.class)
    public Object contentMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    @MetadataScope(keysResolver = TestOutputResolverWithKeyResolver.class, outputResolver = TestOutputResolverWithKeyResolver.class)
    public Object outputMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    @MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class, contentResolver = TestContentAndOutputResolverWithKeyResolver.class, outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
    public Object contentAndOutputMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    @MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class, contentResolver = TestContentAndOutputResolverWithKeyResolver.class, outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
    public Object outputOnlyWithoutContentParam(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
        return null;
    }

    @MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class, contentResolver = TestContentAndOutputResolverWithKeyResolver.class, outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
    public void contentOnlyIgnoresOutput(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
    }

    @MetadataScope(contentResolver = TestContentResolverWithoutKeyResolver.class)
    public Object contentMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    @MetadataScope(outputResolver = TestOutputResolverWithoutKeyResolver.class)
    public Object outputMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    @MetadataScope(contentResolver = TestContentAndOutputResolverWithoutKeyResolver.class, outputResolver = TestContentAndOutputResolverWithoutKeyResolver.class)
    public Object contentAndOutputMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    @MetadataScope(contentResolver = TestContentResolverWithoutKeyResolver.class)
    public void contentMetadataWithoutKeysWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
    }

    @MetadataScope(outputResolver = TestOutputResolverWithoutKeyResolver.class)
    public Object outputMetadataWithoutKeysWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
        return null;
    }

    @MetadataScope(outputResolver = TestResolverWithCache.class, contentResolver = TestResolverWithCache.class)
    public Object contentAndOutputCacheResolver(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    public Object shouldInheritOperationResolvers(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    @MetadataScope(contentResolver = TestResolverWithCache.class)
    public Object contentOnlyCacheResolver(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    @MetadataScope(keysResolver = TestResolverWithCache.class, outputResolver = TestResolverWithCache.class)
    public Object outputAndMetadataKeyCacheResolver(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
        return null;
    }

    @MetadataScope(outputResolver = NullMetadataResolver.class)
    public MuleMessage messageAttributesNullTypeMetadata()
    {
        return null;
    }

    @MetadataScope(outputResolver = NullMetadataResolver.class)
    public MuleMessage<Object, String> messageAttributesPersonTypeMetadata()
    {
        return null;
    }
}