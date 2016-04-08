/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata.extension;

import org.mule.extension.api.annotation.metadata.MetadataScope;
import org.mule.extension.api.annotation.param.Connection;
import org.mule.extension.api.annotation.metadata.Content;
import org.mule.extension.api.annotation.metadata.MetadataKeyParam;
import org.mule.module.extension.internal.metadata.extension.resolver.TestContentResolverWithoutKeyResolver;
import org.mule.module.extension.internal.metadata.extension.resolver.TestMetadataResolverMetadataResolvingFailure;
import org.mule.module.extension.internal.metadata.extension.resolver.TestMetadataResolverRuntimeExceptionFailure;

public class MetadataFailureOperations extends MetadataOperationsParent
{

    // MetadataResolver throws MetadataResolvingException
    @MetadataScope( keysResolver = TestMetadataResolverMetadataResolvingFailure.class,
                    contentResolver = TestMetadataResolverMetadataResolvingFailure.class,
                    outputResolver = TestMetadataResolverMetadataResolvingFailure.class)
    public Object failWithResolvingException(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // With keysResolver resolver and without KeyParam
    @MetadataScope(contentResolver = TestContentResolverWithoutKeyResolver.class)
    public void keyParamWithoutKeyResolver(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
    }

    // Resolver for content and output type
    // With keysResolver and KeyParam
    @MetadataScope( keysResolver = TestMetadataResolverRuntimeExceptionFailure.class,
                    contentResolver = TestMetadataResolverRuntimeExceptionFailure.class,
                    outputResolver = TestMetadataResolverRuntimeExceptionFailure.class)
    public Object failWithRuntimeException(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

}
