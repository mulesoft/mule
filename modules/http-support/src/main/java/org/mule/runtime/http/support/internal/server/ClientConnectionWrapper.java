/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import org.mule.sdk.api.http.domain.message.request.ClientConnection;

import java.net.InetSocketAddress;
import java.security.cert.Certificate;

public class ClientConnectionWrapper implements ClientConnection {

  private final org.mule.runtime.http.api.domain.request.ClientConnection muleClientConnection;

  public ClientConnectionWrapper(org.mule.runtime.http.api.domain.request.ClientConnection muleClientConnection) {
    this.muleClientConnection = muleClientConnection;
  }

  @Override
  public InetSocketAddress getRemoteHostAddress() {
    return muleClientConnection.getRemoteHostAddress();
  }

  @Override
  public Certificate getClientCertificate() {
    return muleClientConnection.getClientCertificate();
  }
}
