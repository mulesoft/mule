/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.adapter;

import org.mule.sdk.api.runtime.process.VoidCompletionCallback;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback} into a
 * {@link VoidCompletionCallback}
 *
 * @since 4.5.0
 */
public class SdkVoidCompletionCallbackAdapter implements VoidCompletionCallback {

  private org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback delegate;

  public SdkVoidCompletionCallbackAdapter(org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback delegate) {
    this.delegate = delegate;
  }

  @Override
  public void success() {
    delegate.success();
  }

  @Override
  public void error(Throwable throwable) {
    delegate.error(throwable);
  }
}
