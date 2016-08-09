/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.model.animals.Animal;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Rectangle;
import org.mule.test.metadata.extension.model.shapes.Shape;
import org.mule.test.metadata.extension.resolver.TestContentAndOutputResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestContentAndOutputResolverWithoutKeyResolverAndKeyIdParam;
import org.mule.test.metadata.extension.resolver.TestContentResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestContentResolverWithoutKeyResolver;
import org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputAttributesResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputNullTypeResolver;
import org.mule.test.metadata.extension.resolver.TestOutputResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputResolverWithoutKeyResolver;
import org.mule.test.metadata.extension.resolver.TestResolverWithCache;
import org.mule.test.metadata.extension.resolver.TestThreadContextClassLoaderResolver;

@MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
    contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
    outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
public class MetadataOperations extends MetadataOperationsParent {

  @MetadataScope(keysResolver = TestContentResolverWithKeyResolver.class,
      contentResolver = TestContentResolverWithKeyResolver.class, outputResolver = TestOutputNullTypeResolver.class)
  public Object contentMetadataWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                         @Optional @Content Object content) {
    return null;
  }

  @MetadataScope(keysResolver = TestOutputResolverWithKeyResolver.class, outputResolver = TestOutputResolverWithKeyResolver.class)
  public Object outputMetadataWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                        @Optional @Content Object content) {
    return null;
  }

  @MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
      contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
      outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
  public Object contentAndOutputMetadataWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                                  @Optional @Content Object content) {
    return null;
  }

  @MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
      contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
      outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
  public Object outputOnlyWithoutContentParam(@Connection MetadataConnection connection, @MetadataKeyId String type) {
    return type;
  }

  @MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
      contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
      outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
  public void contentOnlyIgnoresOutput(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                       @Optional @Content Object content) {}

  @MetadataScope(contentResolver = TestContentAndOutputResolverWithoutKeyResolverAndKeyIdParam.class,
      outputResolver = TestOutputNullTypeResolver.class)
  public Object contentMetadataWithoutKeyId(@Connection MetadataConnection connection, @Optional @Content Object content) {
    return null;
  }

  @MetadataScope(outputResolver = TestContentAndOutputResolverWithoutKeyResolverAndKeyIdParam.class)
  public Object outputMetadataWithoutKeyId(@Connection MetadataConnection connection, @Optional @Content Object content) {
    return null;
  }

  @MetadataScope(contentResolver = TestContentAndOutputResolverWithoutKeyResolverAndKeyIdParam.class,
      outputResolver = TestContentAndOutputResolverWithoutKeyResolverAndKeyIdParam.class)
  public Object contentAndOutputMetadataWithoutKeyId(@Connection MetadataConnection connection,
                                                     @Optional @Content Object content) {
    return null;
  }

  @MetadataScope(contentResolver = TestContentResolverWithoutKeyResolver.class)
  public void contentMetadataWithoutKeysWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                                  @Optional @Content Object content) {}

  @MetadataScope(outputResolver = TestOutputResolverWithoutKeyResolver.class)
  public Object outputMetadataWithoutKeysWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type) {
    return null;
  }

  @MetadataScope(outputResolver = TestResolverWithCache.class, contentResolver = TestResolverWithCache.class)
  public Object contentAndOutputCacheResolver(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                              @Optional @Content Object content) {
    return null;
  }

  public Object shouldInheritOperationResolvers(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                                @Optional @Content Object content) {
    return null;
  }

  @MetadataScope(contentResolver = TestResolverWithCache.class, outputResolver = TestOutputNullTypeResolver.class)
  public Object contentOnlyCacheResolver(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                         @Optional @Content Object content) {
    return null;
  }

  @MetadataScope(keysResolver = TestResolverWithCache.class, outputResolver = TestResolverWithCache.class)
  public Object outputAndMetadataKeyCacheResolver(@Connection MetadataConnection connection, @MetadataKeyId String type) {
    return null;
  }

  @MetadataScope(keysResolver = TestMultiLevelKeyResolver.class, contentResolver = TestMultiLevelKeyResolver.class)
  public LocationKey simpleMultiLevelKeyResolver(@Connection MetadataConnection connection,
                                                 @ParameterGroup @MetadataKeyId LocationKey locationKey,
                                                 @Optional @Content Object content) {
    return locationKey;
  }

  @MetadataScope(outputResolver = TestOutputNullTypeResolver.class)
  public OperationResult messageAttributesNullTypeMetadata() {
    return null;
  }

  @MetadataScope(outputResolver = TestOutputResolverWithoutKeyResolver.class)
  public OperationResult<Object, StringAttributes> messageAttributesPersonTypeMetadata(@MetadataKeyId String type) {
    return null;
  }

  @MetadataScope(keysResolver = TestThreadContextClassLoaderResolver.class)
  public void resolverTypeKeysWithContextClassLoader(@MetadataKeyId String type) {}

  @MetadataScope(contentResolver = TestThreadContextClassLoaderResolver.class)
  public void resolverContentWithContextClassLoader(@Optional @Content Object content, @MetadataKeyId String type) {}

  @MetadataScope(outputResolver = TestThreadContextClassLoaderResolver.class)
  public Object resolverOutputWithContextClassLoader(@MetadataKeyId String type) {
    return null;
  }

  @MetadataScope(keysResolver = TestOutputAttributesResolverWithKeyResolver.class,
      outputResolver = TestOutputAttributesResolverWithKeyResolver.class,
      attributesResolver = TestOutputAttributesResolverWithKeyResolver.class)
  public OperationResult<Object, AbstractOutputAttributes> outputAttributesWithDynamicMetadata(@MetadataKeyId String type) {
    return null;
  }

  @MetadataScope()
  public boolean typeWithDeclaredSubtypesMetadata(Shape plainShape, Rectangle rectangleSubtype, Animal animal) {
    return false;
  }

  @MetadataScope()
  public OperationResult<Shape, AbstractOutputAttributes> outputAttributesWithDeclaredSubtypesMetadata() {
    return null;
  }
}
