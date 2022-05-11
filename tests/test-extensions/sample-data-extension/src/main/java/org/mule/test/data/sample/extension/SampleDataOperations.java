/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.test.data.sample.extension.SampleDataExtension.NULL_VALUE;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.test.data.sample.extension.metadata.JsonTypeResolver;
import org.mule.test.data.sample.extension.metadata.XmlTypeResolver;
import org.mule.test.data.sample.extension.provider.ComplexActingParameterSampleDataProvider;
import org.mule.test.data.sample.extension.provider.ComplexTypeSampleDataProvider;
import org.mule.test.data.sample.extension.provider.ConfigAwareTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.ConnectedTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.FailingTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.GroupTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.MoreComplexTypeSampleDataProvider;
import org.mule.test.data.sample.extension.provider.MuleContextAwareSampleDataProvider;
import org.mule.test.data.sample.extension.provider.OptionalTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.ParameterizedTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.SimplestTestSampleDataProvider;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class SampleDataOperations {

  @SampleData(SimplestTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> parameterLess() {
    return Result.<String, String>builder()
        .output("")
        .mediaType(APPLICATION_JSON)
        .attributes(NULL_VALUE)
        .attributesMediaType(APPLICATION_XML)
        .build();
  }

  @SampleData(ParameterizedTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> connectionLess(String payload, String attributes) {
    return Result.<String, String>builder()
        .output(payload)
        .mediaType(APPLICATION_JSON)
        .attributes(attributes != null ? attributes : NULL_VALUE)
        .attributesMediaType(APPLICATION_XML)
        .build();
  }

  @SampleData(ConnectedTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> useConnection(@org.mule.sdk.api.annotation.param.Connection SampleDataConnection connection,
                                              String payload,
                                              @Optional String attributes) {
    return connection.getResult(payload, attributes);
  }

  @SampleData(ConfigAwareTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> useConfig(@Config SampleDataExtension config,
                                          @Connection SampleDataConnection connection,
                                          String payload,
                                          String attributes) {
    return connection.getResult(config.getPrefix() + payload, config.getPrefix() + attributes);
  }

  @SampleData(ConnectedTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public void nonBlocking(@Connection SampleDataConnection connection,
                          String payload,
                          String attributes,
                          CompletionCallback<String, String> callback) {
    callback.success(useConnection(connection, payload, attributes));
  }

  @SampleData(GroupTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> parameterGroup(@org.mule.sdk.api.annotation.param.Connection SampleDataConnection connection,
                                               @ParameterGroup(name = "group") SampleDataParameterGroup group) {
    return useConnection(connection, group.getGroupParameter(), group.getOptionalParameter());
  }

  @SampleData(GroupTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> showInDslParameterGroup(@Connection SampleDataConnection connection,
                                                        @ParameterGroup(name = "group",
                                                            showInDsl = true) SampleDataParameterGroup group) {
    return parameterGroup(connection, group);
  }

  @SampleData(ParameterizedTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> aliasedGroup(@org.mule.sdk.api.annotation.param.Connection SampleDataConnection connection,
                                             @org.mule.sdk.api.annotation.param.ParameterGroup(
                                                 name = "group") SampleDataAliasedParameterGroup group) {
    return useConnection(connection, group.getPayload(), group.getAttributes());
  }

  @SampleData(MuleContextAwareSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> muleContextAwareSampleData(String payload, String attributes) {
    return connectionLess(payload, attributes);
  }

  @SampleData(ComplexActingParameterSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> complexActingParameter(ComplexActingParameter complex) {
    return connectionLess(complex.getPayload(), complex.getAttributes());
  }


  @SampleData(ComplexTypeSampleDataProvider.class)
  public Result<Map<String, String>, String> complexGenerics(ComplexActingParameter complex) {
    return Result.<Map<String, String>, String>builder().build();
  }

  @SampleData(MoreComplexTypeSampleDataProvider.class)
  public Result<Map<String, List<String>>, String> moreComplexGenerics(ComplexActingParameter complex) {
    return Result.<Map<String, List<String>>, String>builder().build();
  }

  @SampleData(OptionalTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> optionalParameters(@Optional(defaultValue = "[]") String payload, @Optional String attributes) {
    return Result.<String, String>builder()
        .output(payload)
        .mediaType(APPLICATION_JSON)
        .attributes(attributes != null ? attributes : NULL_VALUE)
        .attributesMediaType(APPLICATION_XML)
        .build();
  }

  @SampleData(value = ParameterizedTestSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "payload", extractionExpression = "payload"),
          @Binding(actingParameter = "attributes", extractionExpression = "attributes")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> connectionLessWithTwoBoundActingParameter(String payload, String attributes) {
    return Result.<String, String>builder()
        .output(payload)
        .mediaType(APPLICATION_JSON)
        .attributes(attributes != null ? attributes : NULL_VALUE)
        .attributesMediaType(APPLICATION_XML)
        .build();
  }

  @SampleData(value = ParameterizedTestSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "payload", extractionExpression = "message.payload"),
          @Binding(actingParameter = "attributes", extractionExpression = "message.attributes")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> connectionLessWithTwoBoundActingParameterFromContentField(@TypeResolver(JsonTypeResolver.class) @Content InputStream message)
      throws Exception {
    return Result.<String, String>builder()
        .output("Some payload")
        .mediaType(APPLICATION_JSON)
        .attributes("Some attributes")
        .attributesMediaType(APPLICATION_JSON)
        .build();
  }

  @SampleData(value = ParameterizedTestSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "payload", extractionExpression = "message.nested.payloadXmlTag"),
          @Binding(actingParameter = "attributes", extractionExpression = "message.nested.attributesXmlTag")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> connectionLessWithTwoBoundActingParameterFromXMLContentTag(@TypeResolver(XmlTypeResolver.class) InputStream message) {
    return Result.<String, String>builder()
        .output("Some payload")
        .mediaType(APPLICATION_XML)
        .attributes("Some attributes")
        .attributesMediaType(APPLICATION_XML)
        .build();
  }

  @SampleData(value = ParameterizedTestSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "payload", extractionExpression = "message.nested.xmlTag.@payloadXmlAttribute"),
          @Binding(actingParameter = "attributes", extractionExpression = "message.nested.xmlTag.@attributesXmlAttribute")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> connectionLessWithTwoBoundActingParameterFromXMLContentTagAttribute(@TypeResolver(XmlTypeResolver.class) InputStream message) {
    return Result.<String, String>builder()
        .output("Some payload")
        .mediaType(APPLICATION_XML)
        .attributes("Some attributes")
        .attributesMediaType(APPLICATION_XML)
        .build();
  }

  @SampleData(value = ParameterizedTestSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "payload", extractionExpression = "payloadParameterAlias"),
          @Binding(actingParameter = "attributes", extractionExpression = "attributes")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> connectionLessWithTwoBoundActingParameterOneWithAnAlias(@Alias("payloadParameterAlias") String payload,
                                                                                        String attributes) {
    return Result.<String, String>builder()
        .output(payload)
        .mediaType(APPLICATION_JSON)
        .attributes(attributes != null ? attributes : NULL_VALUE)
        .attributesMediaType(APPLICATION_JSON)
        .build();
  }

  @SampleData(value = ConnectedTestSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "payload", extractionExpression = "payload"),
          @Binding(actingParameter = "attributes", extractionExpression = "attributes")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> useConnectionWithTwoBoundActingParameter(@Connection SampleDataConnection connection,
                                                                         String payload,
                                                                         @Optional String attributes) {
    return connection.getResult(payload, attributes);
  }

  @SampleData(value = ComplexActingParameterSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "complex", extractionExpression = "complex")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> complexBoundActingParameter(ComplexActingParameter complex) {
    return connectionLess(complex.getPayload(), complex.getAttributes());
  }

  @SampleData(value = ComplexActingParameterSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "complex", extractionExpression = "actingParameter.pojoFields")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> pojoBoundActingParameter(@TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {
    return Result.<String, String>builder()
        .output("Some payload")
        .mediaType(APPLICATION_JSON)
        .attributes("Some attributes")
        .attributesMediaType(APPLICATION_JSON)
        .build();
  }

  @SampleData(value = ParameterizedTestSampleDataProvider.class,
      bindings = {@Binding(actingParameter = "payload", extractionExpression = "complex.payload"),
          @Binding(actingParameter = "attributes", extractionExpression = "complex.attributes")})
  @MediaType(TEXT_PLAIN)
  public Result<String, String> boundActingParameterFromPojoField(ComplexActingParameter complex) {
    return connectionLess(complex.getPayload(), complex.getAttributes());
  }

  @SampleData(FailingTestSampleDataProvider.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> failingOperation(String payload, String attributes, boolean useCustomSampleDataException,
                                                 boolean withExceptionCause) {
    return null;
  }
}
