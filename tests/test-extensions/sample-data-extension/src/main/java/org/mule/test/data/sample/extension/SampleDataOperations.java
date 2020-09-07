/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;

import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.annotation.param.Connection;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.process.CompletionCallback;
import org.mule.test.data.sample.extension.config.SampleDataConfig;
import org.mule.test.data.sample.extension.resolver.ConfigAwareTestSampleDataProvider;
import org.mule.test.data.sample.extension.resolver.ConnectedTestSampleDataProvider;
import org.mule.test.data.sample.extension.resolver.GroupTestSampleDataProvider;
import org.mule.test.data.sample.extension.resolver.MuleContextAwareSampleDataProvider;
import org.mule.test.data.sample.extension.resolver.ParameterizedTestSampleDataProvider;

public class SampleDataOperations {

  @SampleData(ParameterizedTestSampleDataProvider.class)
  public Result<String, String> connectionLess(String payload, String attributes) {
    return Result.<String, String>builder()
            .output(payload)
            .mediaType(APPLICATION_JSON)
            .attributes(attributes)
            .attributesMediaType(APPLICATION_XML)
            .build();
  }

  @SampleData(ConnectedTestSampleDataProvider.class)
  public Result<String, String> useConnection(@Connection SampleDataConnection connection,
                                              String payload,
                                              @Optional String attributes) {
    return connection.getResult(payload, attributes);
  }

  @SampleData(ConfigAwareTestSampleDataProvider.class)
  public Result<String, String> useConfig(@Config SampleDataConfig config,
                                          @Connection SampleDataConnection connection,
                                          String payload,
                                          String attributes) {
    return connection.getResult(config.getPrefix() + payload, config.getPrefix() + attributes);
  }

  @SampleData(ConnectedTestSampleDataProvider.class)
  public void nonBlocking(@Connection SampleDataConnection connection,
                          String payload,
                          String attributes,
                          CompletionCallback<String, String> callback) {
    callback.success(useConnection(connection, payload, attributes));
  }

  @SampleData(GroupTestSampleDataProvider.class)
  public Result<String, String> parameterGroup(@Connection SampleDataConnection connection,
                                               @ParameterGroup(name = "group") SampleDataParameterGroup group) {
    return useConnection(connection, group.getGroupParameter(), group.getOptionalParameter());
  }

  @SampleData(GroupTestSampleDataProvider.class)
  public Result<String, String> showInDslParameterGroup(@Connection SampleDataConnection connection,
                                                        @ParameterGroup(name = "group", showInDsl = true) SampleDataParameterGroup group) {
    return parameterGroup(connection, group);
  }

  @SampleData(ParameterizedTestSampleDataProvider.class)
  public Result<String, String> aliasedGroup(@Connection SampleDataConnection connection,
                                             @ParameterGroup(name = "group") SampleDataAliasedParameterGroup group) {
    return useConnection(connection, group.getAliasedPayload(), group.getAliasedPayload());
  }

  @SampleData(MuleContextAwareSampleDataProvider.class)
  public Result<String, String> muleContextAwareSampleData(String payload, String attributes) {
    return connectionLess(payload, attributes);
  }
}
