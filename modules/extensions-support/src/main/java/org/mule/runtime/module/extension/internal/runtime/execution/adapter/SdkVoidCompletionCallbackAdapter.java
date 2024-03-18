/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
