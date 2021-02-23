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

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.test.data.sample.extension.provider.ComplexActingParameterSampleDataProvider;
import org.mule.test.data.sample.extension.provider.ConfigAwareTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.ConnectedTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.GroupTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.MuleContextAwareSampleDataProvider;
import org.mule.test.data.sample.extension.provider.OptionalTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.ParameterizedTestSampleDataProvider;
import org.mule.test.data.sample.extension.provider.SimplestTestSampleDataProvider;

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
  public Result<String, String> useConnection(@Connection SampleDataConnection connection,
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
  public Result<String, String> parameterGroup(@Connection SampleDataConnection connection,
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
  public Result<String, String> aliasedGroup(@Connection SampleDataConnection connection,
                                             @ParameterGroup(name = "group") SampleDataAliasedParameterGroup group) {
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
}
