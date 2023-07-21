/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.sdk.api.runtime.connectivity;

import org.mule.runtime.api.connection.ConnectionException;

public interface ReconnectionCallback {

  void success();

  void failed(ConnectionException exception);

  void doSomething();
}
