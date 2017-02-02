/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import org.mule.service.http.api.server.RequestHandlerManager;

public class DefaultRequestHandlerManager implements RequestHandlerManager {

  private final HttpListenerRegistry.PathMap requestHandlerOwner;
  private final HttpListenerRegistry.RequestHandlerMatcherPair requestHandlerMatcherPair;

  public DefaultRequestHandlerManager(HttpListenerRegistry.PathMap requestHandlerOwner,
                                      HttpListenerRegistry.RequestHandlerMatcherPair requestHandlerMatcherPair) {
    this.requestHandlerOwner = requestHandlerOwner;
    this.requestHandlerMatcherPair = requestHandlerMatcherPair;
  }

  @Override
  public void stop() {
    requestHandlerMatcherPair.setIsRunning(false);
  }

  @Override
  public void start() {
    requestHandlerMatcherPair.setIsRunning(true);
  }

  @Override
  public void dispose() {
    requestHandlerOwner.removeRequestHandlerMatcherPair(requestHandlerMatcherPair);
  }
}
