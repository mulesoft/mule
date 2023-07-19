/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.util.Map;

/**
 * An abstract wrapper for {@link Source} implementations that allows to intercept all the invocations related to a generic
 * {@link Source} lifecycle and event handlers.
 *
 * @param <T>
 * @param <A>
 *
 * @since 4.1
 */
public abstract class SourceWrapper<T, A> extends Source<T, A> {

  protected final Source<T, A> delegate;

  public SourceWrapper(Source<T, A> delegate) {
    this.delegate = delegate;
  }

  public Source<T, A> getDelegate() {
    return delegate;
  }

  public void onSuccess(CoreEvent event, Map<String, Object> parameters, SourceCallbackContext context,
                        CompletableCallback<Void> callback) {
    callback.complete(null);
  }

  public void onError(CoreEvent event, Map<String, Object> parameters, SourceCallbackContext context,
                      CompletableCallback<Void> callback) {
    callback.complete(null);
  }

  public void onTerminate(CoreEvent event, Map<String, Object> parameters, SourceCallbackContext context,
                          CompletableCallback<Void> callback) {
    callback.complete(null);
  }

  public void onBackPressure(CoreEvent event, Map<String, Object> parameters, SourceCallbackContext context,
                             CompletableCallback<Void> callback) {
    callback.complete(null);
  }
}
