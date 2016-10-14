/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.Query;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.introspection.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.model.animals.Animal;
import org.mule.test.metadata.extension.model.animals.AnimalClade;
import org.mule.test.metadata.extension.model.animals.Bear;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Rectangle;
import org.mule.test.metadata.extension.model.shapes.Shape;
import org.mule.test.metadata.extension.query.MetadataExtensionEntityResolver;
import org.mule.test.metadata.extension.query.MetadataExtensionQueryTranslator;
import org.mule.test.metadata.extension.query.NativeQueryOutputResolver;
import org.mule.test.metadata.extension.resolver.TestBooleanMetadataResolver;
import org.mule.test.metadata.extension.resolver.TestEnumMetadataResolver;
import org.mule.test.metadata.extension.resolver.TestInputAndOutputResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam;
import org.mule.test.metadata.extension.resolver.TestInputResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestInputResolverWithoutKeyResolver;
import org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputAnyTypeResolver;
import org.mule.test.metadata.extension.resolver.TestOutputAttributesResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestOutputResolverWithoutKeyResolver;
import org.mule.test.metadata.extension.resolver.TestResolverWithCache;
import org.mule.test.metadata.extension.resolver.TestThreadContextClassLoaderResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MetadataOperations {

  @OutputResolver(TestOutputAnyTypeResolver.class)
  public Object contentMetadataWithKeyId(@UseConfig Object object, @Connection MetadataConnection connection,
                                         @MetadataKeyId(TestInputResolverWithKeyResolver.class) String type,
                                         @Optional @Content @TypeResolver(TestInputResolverWithKeyResolver.class) Object content) {
    return null;
  }

  @OutputResolver(TestOutputResolverWithKeyResolver.class)
  public Object outputMetadataWithKeyId(@Connection MetadataConnection connection,
                                        @MetadataKeyId(TestOutputResolverWithKeyResolver.class) String type,
                                        @Optional @Content Object content) {
    return null;
  }

  @OutputResolver(TestOutputResolverWithKeyResolver.class)
  public Object metadataKeyWithDefaultValue(@Connection MetadataConnection connection,
                                            @Optional(
                                                defaultValue = CAR) @MetadataKeyId(TestOutputResolverWithKeyResolver.class) String type,
                                            @Optional @Content Object content) {
    return type;
  }

  @OutputResolver(TestInputAndOutputResolverWithKeyResolver.class)
  public Object contentAndOutputMetadataWithKeyId(@Connection MetadataConnection connection,
                                                  @MetadataKeyId(TestInputAndOutputResolverWithKeyResolver.class) String type,
                                                  @Optional @TypeResolver(TestInputAndOutputResolverWithKeyResolver.class) Object content) {
    return null;
  }


  @OutputResolver(TestInputAndOutputResolverWithKeyResolver.class)
  public Object outputAndMultipleInputWithKeyId(@Connection MetadataConnection connection,
                                                @MetadataKeyId(TestInputAndOutputResolverWithKeyResolver.class) String type,
                                                @Optional @TypeResolver(TestInputAndOutputResolverWithKeyResolver.class) Object firstPerson,
                                                @Optional @TypeResolver(TestInputAndOutputResolverWithKeyResolver.class) Object otherPerson) {
    return null;
  }


  @OutputResolver(TestInputAndOutputResolverWithKeyResolver.class)
  public Object outputOnlyWithoutContentParam(@Connection MetadataConnection connection,
                                              @MetadataKeyId(TestInputAndOutputResolverWithKeyResolver.class) String type) {
    return type;
  }

  public boolean booleanMetadataKey(@Connection MetadataConnection connection, @MetadataKeyId boolean type,
                                    @Optional @TypeResolver(TestBooleanMetadataResolver.class) Object content) {
    return type;
  }

  public AnimalClade enumMetadataKey(@Connection MetadataConnection connection, @MetadataKeyId AnimalClade type,
                                     @Optional @TypeResolver(TestEnumMetadataResolver.class) Object content) {
    return type;
  }

  @OutputResolver(TestInputAndOutputResolverWithKeyResolver.class)
  public void contentOnlyIgnoresOutput(@Connection MetadataConnection connection,
                                       @MetadataKeyId(TestInputAndOutputResolverWithKeyResolver.class) String type,
                                       @Optional @TypeResolver(TestInputAndOutputResolverWithKeyResolver.class) Object content) {}

  @OutputResolver(TestOutputAnyTypeResolver.class)
  public Object contentMetadataWithoutKeyId(@Connection MetadataConnection connection,
                                            @Optional @TypeResolver(TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam.class) Object content) {
    return null;
  }

  @OutputResolver(TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam.class)
  public Object outputMetadataWithoutKeyId(@Connection MetadataConnection connection, @Optional @Content Object content) {
    return null;
  }

  @OutputResolver(TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam.class)
  public Object contentAndOutputMetadataWithoutKeyId(@Connection MetadataConnection connection,
                                                     @Optional @Content @TypeResolver(TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam.class) Object content) {
    return null;
  }

  public void contentMetadataWithoutKeysWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                                  @Optional @TypeResolver(TestInputResolverWithoutKeyResolver.class) Object content) {}

  @OutputResolver(TestOutputResolverWithoutKeyResolver.class)
  public Object outputMetadataWithoutKeysWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type) {
    return null;
  }

  @OutputResolver(TestResolverWithCache.class)
  public Object contentAndOutputCacheResolver(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                              @Optional @TypeResolver(TestResolverWithCache.class) Object content) {
    return null;
  }

  @OutputResolver(TestOutputAnyTypeResolver.class)
  public Object contentOnlyCacheResolver(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                         @Optional @TypeResolver(TestResolverWithCache.class) Object content) {
    return null;
  }

  @OutputResolver(TestResolverWithCache.class)
  public Object outputAndMetadataKeyCacheResolver(@Connection MetadataConnection connection,
                                                  @MetadataKeyId(TestResolverWithCache.class) String type) {
    return null;
  }

  public LocationKey simpleMultiLevelKeyResolver(@Connection MetadataConnection connection,
                                                 @ParameterGroup @MetadataKeyId(TestMultiLevelKeyResolver.class) LocationKey locationKey,
                                                 @Optional @TypeResolver(TestMultiLevelKeyResolver.class) Object content) {
    return locationKey;
  }

  @OutputResolver(TestOutputAnyTypeResolver.class)
  public OperationResult messageAttributesVoidTypeMetadata() {
    return null;
  }

  @OutputResolver(TestOutputResolverWithoutKeyResolver.class)
  public OperationResult<Object, StringAttributes> messageAttributesPersonTypeMetadata(@MetadataKeyId String type) {
    return null;
  }

  public void resolverContentWithContextClassLoader(@Optional @TypeResolver(TestThreadContextClassLoaderResolver.class) Object content,
                                                    @MetadataKeyId(TestThreadContextClassLoaderResolver.class) String type) {}

  @OutputResolver(TestThreadContextClassLoaderResolver.class)
  public Object resolverOutputWithContextClassLoader(@MetadataKeyId String type) {
    return null;
  }

  @OutputResolver(TestOutputAttributesResolverWithKeyResolver.class)
  public OperationResult<Object, AbstractOutputAttributes> outputAttributesWithDynamicMetadata(
                                                                                               @MetadataKeyId(TestOutputAttributesResolverWithKeyResolver.class) String type) {
    return null;
  }

  public boolean typeWithDeclaredSubtypesMetadata(Shape plainShape, Rectangle rectangleSubtype, Animal animal) {
    return false;
  }

  public void contentParameterShouldNotGenerateMapChildElement(@Content @TypeResolver(TestInputResolverWithoutKeyResolver.class) Map<String, Object> mapContent) {}

  public void contentParameterShouldNotGenerateListChildElement(@TypeResolver(TestInputResolverWithoutKeyResolver.class) List<String> listContent) {}

  public void contentParameterShouldNotGeneratePojoChildElement(@TypeResolver(TestInputResolverWithoutKeyResolver.class) Bear animalContent) {}

  @Query(translator = MetadataExtensionQueryTranslator.class,
      entityResolver = MetadataExtensionEntityResolver.class,
      nativeOutputResolver = NativeQueryOutputResolver.class)
  public String doQuery(@MetadataKeyId String query) {
    return query;
  }

  public PagingProvider<MetadataConnection, Animal> pagedOperationMetadata(Animal animal) {
    return new PagingProvider<MetadataConnection, Animal>() {

      @Override
      public List<Animal> getPage(MetadataConnection connection) {
        return Collections.singletonList(animal);
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(MetadataConnection connection) {
        return java.util.Optional.of(1);
      }

      @Override
      public void close() throws IOException {}
    };
  }

  public OperationResult<Shape, AbstractOutputAttributes> outputAttributesWithDeclaredSubtypesMetadata() {
    return null;
  }
}
