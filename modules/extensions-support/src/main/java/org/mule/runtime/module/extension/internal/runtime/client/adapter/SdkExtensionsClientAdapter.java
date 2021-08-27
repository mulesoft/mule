/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.adapter;

import static org.mule.runtime.core.internal.util.message.SdkResultAdapter.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.internal.util.message.SdkResultAdapter;
import org.mule.sdk.api.client.ExtensionsClient;
import org.mule.sdk.api.client.OperationParameters;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.concurrent.CompletableFuture;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.client.ExtensionsClient} into a {@link ExtensionsClient}
 *
 * @since 4.4.0
 */
public class SdkExtensionsClientAdapter implements ExtensionsClient {

  private final org.mule.runtime.extension.api.client.ExtensionsClient delegate;

  public SdkExtensionsClientAdapter(org.mule.runtime.extension.api.client.ExtensionsClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension, String operation, OperationParameters parameters) {
    return delegate.executeAsync(extension, operation, new LegacyOperationParametersAdapter(parameters))
        .thenApply(SdkResultAdapter::from);
  }

  @Override
  public <T, A> Result<T, A> execute(String extension, String operation, OperationParameters parameters) throws MuleException {
    return from(delegate.execute(extension, operation, new LegacyOperationParametersAdapter(parameters)));
  }
}
