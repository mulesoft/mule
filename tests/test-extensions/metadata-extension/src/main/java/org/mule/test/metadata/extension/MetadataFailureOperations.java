/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.test.metadata.extension.resolver.TestMetadataResolverMetadataResolvingFailure;
import org.mule.test.metadata.extension.resolver.TestMetadataResolverRuntimeExceptionFailure;

public class MetadataFailureOperations extends MetadataOperationsParent {

  // NamedTypeResolver throws MetadataResolvingException
  @OutputResolver(output = TestMetadataResolverMetadataResolvingFailure.class)
  public Object failWithResolvingException(@Connection MetadataConnection connection,
                                           @MetadataKeyId(TestMetadataResolverMetadataResolvingFailure.class) String type,
                                           @Content @TypeResolver(TestMetadataResolverMetadataResolvingFailure.class) Object content) {
    return null;
  }

  // With keysResolver resolver and without KeyParam
  public void keyIdWithoutKeyResolver(@Connection MetadataConnection connection, @MetadataKeyId String type) {}

  // Resolver for content and output type
  // With keysResolver and KeyParam
  @OutputResolver(output = TestMetadataResolverRuntimeExceptionFailure.class)
  public Object failWithRuntimeException(@Connection MetadataConnection connection,
                                         @MetadataKeyId(TestMetadataResolverRuntimeExceptionFailure.class) String type,
                                         @Content @TypeResolver(TestMetadataResolverRuntimeExceptionFailure.class) Object content) {
    return null;
  }

}
