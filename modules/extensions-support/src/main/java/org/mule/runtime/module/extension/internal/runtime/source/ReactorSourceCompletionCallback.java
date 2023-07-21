/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link SourceCompletionCallback} which works using a supplied {@link CompletableCallback}
 *
 * @since 4.3.0
 */
final class CompletableSourceCompletionCallback implements SourceCompletionCallback {

  private final CompletableCallback<Void> callback;

  /**
   * Creates a new instance
   * 
   * @param callback a {@link CompletableFuture} to be completed through this callback
   */
  public CompletableSourceCompletionCallback(CompletableCallback<Void> callback) {
    this.callback = callback;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void success() {
    callback.complete(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void error(Throwable t) {
    callback.error(t);
  }
}
