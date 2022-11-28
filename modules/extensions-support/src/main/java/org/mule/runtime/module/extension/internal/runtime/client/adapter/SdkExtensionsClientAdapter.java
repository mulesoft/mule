/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.adapter;

import org.mule.runtime.core.internal.util.message.SdkResultAdapter;
import org.mule.sdk.api.client.ExtensionsClient;
import org.mule.sdk.api.client.OperationParameterizer;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.client.ExtensionsClient} into a {@link ExtensionsClient}
 *
 * @since 4.5.0
 */
public class SdkExtensionsClientAdapter implements ExtensionsClient {

  private final org.mule.runtime.extension.api.client.ExtensionsClient delegate;

  public SdkExtensionsClientAdapter(org.mule.runtime.extension.api.client.ExtensionsClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension,
                                                             String operation,
                                                             Consumer<OperationParameterizer> parameters) {
    CompletableFuture<Result<T, A>> future = new CompletableFuture<>();
    delegate.executeAsync(extension, operation, realParams -> parameters.accept(new SdkOperationParameterizerAdapter(realParams)))
        .whenComplete((v, e) -> {
          if (e != null) {
            future.completeExceptionally(e);
          } else {
            future.complete(SdkResultAdapter.from(v));
          }
        });

    return future;
  }
}
