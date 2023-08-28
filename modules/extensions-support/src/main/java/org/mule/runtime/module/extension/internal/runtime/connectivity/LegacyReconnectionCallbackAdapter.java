/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.runtime.connectivity.ReconnectionCallback;

/**
 * Adapts a {@link org.mule.sdk.api.runtime.connectivity.ReconnectionCallback} into a legacy {@link ReconnectionCallback}
 *
 * @since 4.5.0
 */
public class LegacyReconnectionCallbackAdapter implements ReconnectionCallback {

  private final org.mule.sdk.api.runtime.connectivity.ReconnectionCallback delegate;

  public LegacyReconnectionCallbackAdapter(org.mule.sdk.api.runtime.connectivity.ReconnectionCallback reconnectionCallback) {
    this.delegate = reconnectionCallback;
  }

  @Override
  public void success() {
    delegate.success();
  }

  @Override
  public void failed(ConnectionException exception) {
    delegate.failed(exception);
  }
}
