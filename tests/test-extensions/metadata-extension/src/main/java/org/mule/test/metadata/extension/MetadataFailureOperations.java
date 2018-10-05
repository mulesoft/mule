/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.test.metadata.extension.resolver.TestMetadataResolverMetadataResolvingFailure;
import org.mule.test.metadata.extension.resolver.TestMetadataResolverRuntimeExceptionFailure;

public class MetadataFailureOperations extends MetadataOperationsParent {

  // NamedTypeResolver throws MetadataResolvingException
  @OutputResolver(output = TestMetadataResolverMetadataResolvingFailure.class)
  @MediaType(value = TEXT_PLAIN, strict = false)
  public Object failWithResolvingException(@Connection MetadataConnection connection,
                                           @MetadataKeyId(TestMetadataResolverMetadataResolvingFailure.class) String type,
                                           @Content @TypeResolver(TestMetadataResolverMetadataResolvingFailure.class) Object content) {
    return null;
  }

  // Resolver for content and output type
  // With keysResolver and KeyParam
  @OutputResolver(output = TestMetadataResolverRuntimeExceptionFailure.class)
  @MediaType(value = TEXT_PLAIN, strict = false)
  public Object failWithRuntimeException(@Connection MetadataConnection connection,
                                         @MetadataKeyId(TestMetadataResolverRuntimeExceptionFailure.class) String type,
                                         @Content @TypeResolver(TestMetadataResolverRuntimeExceptionFailure.class) Object content) {
    return null;
  }

}
