/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.server;

import org.mule.sdk.api.http.server.EndpointAvailabilityHandler;

public class EndpointAvailabilityHandlerWrapper implements EndpointAvailabilityHandler {

  private final org.mule.runtime.http.api.server.RequestHandlerManager delegate;

  public EndpointAvailabilityHandlerWrapper(org.mule.runtime.http.api.server.RequestHandlerManager delegate) {
    this.delegate = delegate;
  }

  @Override
  public void unavailable() {
    delegate.stop();
  }

  @Override
  public void available() {
    delegate.start();
  }

  @Override
  public void remove() {
    delegate.dispose();
  }
}
