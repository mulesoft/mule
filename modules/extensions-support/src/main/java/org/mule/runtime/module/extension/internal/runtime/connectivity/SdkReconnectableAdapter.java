/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.runtime.connectivity.Reconnectable;
import org.mule.sdk.api.runtime.connectivity.ReconnectionCallback;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.connectivity.Reconnectable} into an sdk-api {@link Reconnectable}
 *
 * @since 4.5.0
 */
public class SdkReconnectableAdapter implements Reconnectable {

  private final org.mule.runtime.extension.api.runtime.connectivity.Reconnectable delegate;

  public static Reconnectable from(Object value) {
    if (value instanceof Reconnectable) {
      return (Reconnectable) value;
    } else if (value instanceof org.mule.runtime.extension.api.runtime.connectivity.Reconnectable) {
      return new SdkReconnectableAdapter((org.mule.runtime.extension.api.runtime.connectivity.Reconnectable) value);
    } else {
      return null;
    }
  }

  public SdkReconnectableAdapter(org.mule.runtime.extension.api.runtime.connectivity.Reconnectable reconnectable) {
    this.delegate = reconnectable;
  }

  @Override
  public void reconnect(ConnectionException exception, ReconnectionCallback reconnectionCallback) {
    delegate.reconnect(exception, new LegacyReconnectionCallbackAdapter(reconnectionCallback));
  }
}
