/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import org.mule.extension.http.api.error.HttpError;
import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.http.internal.request.client.HttpExtensionClient;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Set;

/**
 * Errors that can be thrown in the {@link HttpRequestOperations#request(String, String, ConfigurationOverrides,
 * ResponseValidationSettings, HttpRequesterRequestBuilder, OutputSettings, HttpExtensionClient, HttpRequesterConfig,
 * CompletionCallback)} operation.
 *
 * @since 4.0
 */
public class RequestErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    //TODO: MULE-11506 - Clean up once the error hierarchy is considered and only RESPONSE_VALIDATION necessary
    ImmutableSet.Builder<ErrorTypeDefinition> setBuilder = ImmutableSet.builder();
    Arrays.stream(HttpError.values()).forEach(setBuilder::add);
    return setBuilder.build();
  }
}
