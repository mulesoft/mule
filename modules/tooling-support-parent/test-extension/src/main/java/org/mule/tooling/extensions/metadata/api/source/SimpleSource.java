/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tooling.extensions.metadata.api.source;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.tooling.extensions.metadata.internal.config.SimpleConfiguration;
import org.mule.tooling.extensions.metadata.internal.connection.TstExtensionClient;
import org.mule.tooling.extensions.metadata.internal.metadata.MultiLevelPartialTypeKeysOutputTypeResolver;
import org.mule.tooling.extensions.metadata.api.parameters.LocationKey;
import org.mule.tooling.extensions.metadata.internal.value.ActingParameterVP;
import org.mule.tooling.extensions.metadata.internal.value.ConfigLessConnectionLessNoActingParamVP;
import org.mule.tooling.extensions.metadata.internal.value.ConfigLessNoActingParamVP;

@Alias("simple")
@MediaType(value = "text/plain")
@MetadataScope(keysResolver = MultiLevelPartialTypeKeysOutputTypeResolver.class,
        outputResolver = MultiLevelPartialTypeKeysOutputTypeResolver.class,
attributesResolver = MultiLevelPartialTypeKeysOutputTypeResolver.class)
public class SimpleSource extends Source<String, StringAttributes> {

  @Config
  private SimpleConfiguration config;

  @Connection
  private ConnectionProvider<TstExtensionClient> connection;

  @Parameter
  public String actingParameter;

  @Parameter
  @OfValues(ConfigLessConnectionLessNoActingParamVP.class)
  public String independentParam;

  @Parameter
  @OfValues(ConfigLessNoActingParamVP.class)
  public String connectionDependantParam;

  @Parameter
  @OfValues(ActingParameterVP.class)
  public String actingParameterDependantParam;

  @ParameterGroup(name="LocationKey")
  @MetadataKeyId
  public LocationKey locationKey;

  @Override
  public void onStart(SourceCallback<String, StringAttributes> sourceCallback) throws MuleException { }

  @OnSuccess
  public void onSuccess(@TypeResolver(MultiLevelPartialTypeKeysOutputTypeResolver.class) String onSuccessParameter,
                        SourceCallbackContext callbackContext)
          throws Exception {
  }

  @OnTerminate
  public void onTerminate() {}

  @Override
  public void onStop() { }

}
