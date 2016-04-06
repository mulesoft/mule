/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata.extension;

import org.mule.api.temporary.MuleMessage;
import org.mule.extension.api.annotation.metadata.MetadataScope;
import org.mule.extension.api.annotation.param.Connection;
import org.mule.extension.api.annotation.metadata.Content;
import org.mule.extension.api.annotation.metadata.MetadataKeyParam;
import org.mule.module.extension.internal.metadata.extension.resolver.TestContentAndOutputResolverWithKeyResolver;
import org.mule.module.extension.internal.metadata.extension.resolver.TestContentAndOutputResolverWithoutKeyResolver;
import org.mule.module.extension.internal.metadata.extension.resolver.TestContentResolverWithKeyResolver;
import org.mule.module.extension.internal.metadata.extension.resolver.TestContentResolverWithoutKeyResolver;
import org.mule.module.extension.internal.metadata.extension.resolver.TestOutputResolverWithKeyResolver;
import org.mule.module.extension.internal.metadata.extension.resolver.TestOutputResolverWithoutKeyResolver;
import org.mule.module.extension.internal.metadata.extension.resolver.TestResolverWithCache;

public class MetadataOperations
{

    // Resolver for content only, ignores Object return type
    // With keysResolver and KeyParam
    @MetadataScope( keysResolver = TestContentResolverWithKeyResolver.class,
                    contentResolver = TestContentResolverWithKeyResolver.class)
    public Object contentMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // Resolver for output only, ignores @Content Object
    // With keysResolver and KeyParam
    @MetadataScope( keysResolver = TestOutputResolverWithKeyResolver.class,
                    outputResolver = TestOutputResolverWithKeyResolver.class)
    public Object outputMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // Resolver for content and output type
    // With keysResolver and KeyParam
    @MetadataScope( keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
                    contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
                    outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
    public Object contentAndOutputMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // Resolver for content and output type, no @Content param, resolves only output
    // With keysResolver and KeyParam
    @MetadataScope( keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
                    contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
                    outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
    public Object outputOnlyWithoutContentParam(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
        return null;
    }

    // Resolver for content and output type, no return type, resolves only @Content
    // With key and KeyParam
    @MetadataScope( keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
                    contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
                    outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
    public void contentOnlyIgnoresOutput(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
    }


    // Resolver for content only, ignores Object return type
    // Without keysResolver and KeyParam
    @MetadataScope(contentResolver = TestContentResolverWithoutKeyResolver.class)
    public Object contentMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    // Resolver for output only, ignores @Content Object
    // Without keysResolver and KeyParam
    @MetadataScope(outputResolver = TestOutputResolverWithoutKeyResolver.class)
    public Object outputMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    // Resolver for content and output types
    // Without keysResolver and KeyParam
    @MetadataScope( contentResolver = TestContentAndOutputResolverWithoutKeyResolver.class,
                    outputResolver = TestContentAndOutputResolverWithoutKeyResolver.class)
    public Object contentAndOutputMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    // Resolver for content only
    // Without keysResolver
    // With KeyParam
    @MetadataScope(contentResolver = TestContentResolverWithoutKeyResolver.class)
    public void contentMetadataWithoutKeysWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
    }

    // Resolver for output only
    // Without keysResolver
    // With KeyParam
    @MetadataScope(outputResolver = TestOutputResolverWithoutKeyResolver.class)
    public Object outputMetadataWithoutKeysWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
        return null;
    }

    // Resolver for output and content
    // Without keysResolver
    // With KeyParam
    // Uses Cache
    @MetadataScope(outputResolver = TestResolverWithCache.class, contentResolver = TestResolverWithCache.class)
    public Object contentAndOutputCacheResolver(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // Resolver for content
    // Without keysResolver
    // With KeyParam
    // Uses Cache
    @MetadataScope(contentResolver = TestResolverWithCache.class)
    public Object contentOnlyCacheResolver(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    public MuleMessage messageAttributesNullTypeMetadata()
    {
        return null;
    }

    public MuleMessage<Object, String> messageAttributesPersonTypeMetadata()
    {
        return null;
    }
}