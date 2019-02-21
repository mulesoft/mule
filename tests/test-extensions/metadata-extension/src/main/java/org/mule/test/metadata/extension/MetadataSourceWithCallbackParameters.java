/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import static org.mule.test.metadata.extension.MetadataConnection.PERSON;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.model.response.ErrorResponse;
import org.mule.test.metadata.extension.model.response.SuccessResponse;
import org.mule.test.metadata.extension.resolver.TestInputAndOutputWithAttributesResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestInputResolver;
import org.mule.test.metadata.extension.resolver.TestMetadataInputCarResolver;

import java.util.Map;


@MetadataScope(keysResolver = TestInputOutputSourceResolverWithKeyResolver.class,
    outputResolver = TestInputAndOutputWithAttributesResolverWithKeyResolver.class)
public class MetadataSourceWithCallbackParameters extends Source<Map<String, Object>, StringAttributes> {

  public static boolean STARTED = false;

  @MetadataKeyId
  @Parameter
  public String type;

  @Connection
  private ConnectionProvider<MetadataConnection> connection;

  @Override
  public void onStart(SourceCallback<Map<String, Object>, StringAttributes> sourceCallback) throws MuleException {
    STARTED = true;
    if (!type.equals(PERSON)) {
      throw new RuntimeException(String.format("Invalid MetadataKey with value [%s], the key should be [%s]", type, PERSON));
    }
  }

  @OnSuccess
  public void onSuccess(@ParameterGroup(name = "Response",
      showInDsl = true) SuccessResponse successResponse,
                        @TypeResolver(TestMetadataInputCarResolver.class) Object successObject,
                        SourceCallbackContext callbackContext,
                        SourceCompletionCallback completionCallback) {
    // Do nothing.
  }

  @OnError
  public void onError(@ParameterGroup(name = "Error Response",
      showInDsl = true) ErrorResponse errorResponse,
                      @TypeResolver(TestInputResolver.class) Object errorObject,
                      SourceCallbackContext callbackContext,
                      SourceCompletionCallback completionCallback) {
    // Do nothing.
  }

  @Override
  public void onStop() {
    STARTED = false;
  }
}
