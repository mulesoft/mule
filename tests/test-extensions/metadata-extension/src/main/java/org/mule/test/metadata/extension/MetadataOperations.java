/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Query;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
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

  @OutputResolver(output = TestOutputAnyTypeResolver.class)
  public Object contentMetadataWithKeyId(@Config Object object, @Connection MetadataConnection connection,
                                         @MetadataKeyId(TestInputResolverWithKeyResolver.class) String type,
                                         @Optional @Content @TypeResolver(TestInputResolverWithKeyResolver.class) Object content) {
    return null;
  }

  @OutputResolver(output = TestOutputResolverWithKeyResolver.class)
  public Object outputMetadataWithKeyId(@Connection MetadataConnection connection,
                                        @MetadataKeyId(TestOutputResolverWithKeyResolver.class) String type,
                                        @Optional @Content Object content) {
    return null;
  }

  @OutputResolver(output = TestOutputResolverWithKeyResolver.class)
  public Object metadataKeyWithDefaultValue(@Connection MetadataConnection connection,
                                            @Optional(
                                                defaultValue = CAR) @MetadataKeyId(TestOutputResolverWithKeyResolver.class) String type,
                                            @Optional @Content Object content) {
    return type;
  }

  @OutputResolver(output = TestInputAndOutputResolverWithKeyResolver.class)
  public Object contentAndOutputMetadataWithKeyId(@Connection MetadataConnection connection,
                                                  @MetadataKeyId(TestInputAndOutputResolverWithKeyResolver.class) String type,
                                                  @Optional @TypeResolver(TestInputAndOutputResolverWithKeyResolver.class) Object content) {
    return null;
  }


  @OutputResolver(output = TestInputAndOutputResolverWithKeyResolver.class)
  public Object outputAndMultipleInputWithKeyId(@Connection MetadataConnection connection,
                                                @MetadataKeyId(TestInputAndOutputResolverWithKeyResolver.class) String type,
                                                @Optional @TypeResolver(TestInputAndOutputResolverWithKeyResolver.class) Object firstPerson,
                                                @Optional @TypeResolver(TestInputAndOutputResolverWithKeyResolver.class) Object otherPerson) {
    return null;
  }


  @OutputResolver(output = TestInputAndOutputResolverWithKeyResolver.class)
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

  @OutputResolver(output = TestInputAndOutputResolverWithKeyResolver.class)
  public void contentOnlyIgnoresOutput(@Connection MetadataConnection connection,
                                       @MetadataKeyId(TestInputAndOutputResolverWithKeyResolver.class) String type,
                                       @Optional @TypeResolver(TestInputAndOutputResolverWithKeyResolver.class) Object content) {}

  @OutputResolver(output = TestOutputAnyTypeResolver.class)
  public Object contentMetadataWithoutKeyId(@Connection MetadataConnection connection,
                                            @Optional @TypeResolver(TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam.class) Object content) {
    return null;
  }

  @OutputResolver(output = TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam.class)
  public Object outputMetadataWithoutKeyId(@Connection MetadataConnection connection, @Optional @Content Object content) {
    return null;
  }

  @OutputResolver(output = TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam.class)
  public Object contentAndOutputMetadataWithoutKeyId(@Connection MetadataConnection connection,
                                                     @Optional @Content @TypeResolver(TestInputAndOutputResolverWithoutKeyResolverAndKeyIdParam.class) Object content) {
    return null;
  }

  public void contentMetadataWithoutKeysWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                                  @Optional @TypeResolver(TestInputResolverWithoutKeyResolver.class) Object content) {}

  @OutputResolver(output = TestOutputResolverWithoutKeyResolver.class)
  public Object outputMetadataWithoutKeysWithKeyId(@Connection MetadataConnection connection, @MetadataKeyId String type) {
    return null;
  }

  @OutputResolver(output = TestResolverWithCache.class)
  public Object contentAndOutputCacheResolver(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                              @Optional @TypeResolver(TestResolverWithCache.class) Object content) {
    return null;
  }

  @OutputResolver(output = TestOutputAnyTypeResolver.class)
  public Object contentOnlyCacheResolver(@Connection MetadataConnection connection, @MetadataKeyId String type,
                                         @Optional @TypeResolver(TestResolverWithCache.class) Object content) {
    return null;
  }

  @OutputResolver(output = TestResolverWithCache.class)
  public Object outputAndMetadataKeyCacheResolver(@Connection MetadataConnection connection,
                                                  @MetadataKeyId(TestResolverWithCache.class) String type) {
    return null;
  }

  public LocationKey simpleMultiLevelKeyResolver(@Connection MetadataConnection connection,
                                                 @ParameterGroup(
                                                     name = "key") @MetadataKeyId(TestMultiLevelKeyResolver.class) LocationKey locationKey,
                                                 @Optional @TypeResolver(TestMultiLevelKeyResolver.class) Object content) {
    return locationKey;
  }

  @OutputResolver(output = TestOutputAnyTypeResolver.class)
  public Result messageAttributesVoidTypeMetadata() {
    return null;
  }

  @OutputResolver(output = TestOutputResolverWithoutKeyResolver.class)
  public Result<Object, StringAttributes> messageAttributesPersonTypeMetadata(@MetadataKeyId String type) {
    return null;
  }

  public void resolverContentWithContextClassLoader(
                                                    @Optional @TypeResolver(TestThreadContextClassLoaderResolver.class) Object content,
                                                    @MetadataKeyId(TestThreadContextClassLoaderResolver.class) String type) {}

  @OutputResolver(output = TestThreadContextClassLoaderResolver.class)
  public Object resolverOutputWithContextClassLoader(@MetadataKeyId String type) {
    return null;
  }

  @OutputResolver(output = TestOutputAttributesResolverWithKeyResolver.class,
      attributes = TestOutputAttributesResolverWithKeyResolver.class)
  public Result<Object, AbstractOutputAttributes> outputAttributesWithDynamicMetadata(
                                                                                      @MetadataKeyId(TestOutputAttributesResolverWithKeyResolver.class) String type) {
    return null;
  }

  public List<Result<String, StringAttributes>> listOfMessages() {
    return null;
  }

  @OutputResolver(output = TestOutputResolverWithoutKeyResolver.class)
  public List<Result> dynamicListOfMessages(@MetadataKeyId String type) {
    return null;
  }

  public boolean typeWithDeclaredSubtypesMetadata(Shape plainShape, Rectangle rectangleSubtype, Animal animal) {
    return false;
  }

  public void contentParameterShouldNotGenerateMapChildElement(
                                                               @Content @TypeResolver(TestInputResolverWithoutKeyResolver.class) Map<String, Object> mapContent) {}

  public void contentParameterShouldNotGenerateListChildElement(
                                                                @TypeResolver(TestInputResolverWithoutKeyResolver.class) List<String> contents) {}

  public void contentParameterShouldNotGeneratePojoChildElement(
                                                                @TypeResolver(TestInputResolverWithoutKeyResolver.class) Bear animalContent) {}

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

  public Result<Shape, AbstractOutputAttributes> outputAttributesWithDeclaredSubtypesMetadata() {
    return null;
  }
}
