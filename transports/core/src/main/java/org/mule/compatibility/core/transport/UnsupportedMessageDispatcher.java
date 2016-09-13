/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;

public final class UnsupportedMessageDispatcher extends AbstractMessageDispatcher {

  public UnsupportedMessageDispatcher(OutboundEndpoint endpoint) {
    super(endpoint);
  }

  @Override
  protected void doDispatch(Event event) throws Exception {
    throw new UnsupportedOperationException("Dispatch not supported for this transport.");
  }

  @Override
  protected InternalMessage doSend(Event event) throws Exception {
    throw new UnsupportedOperationException("Send not supported for this transport.");
  }

  @Override
  protected void doInitialise() {
    // empty
  }

  @Override
  protected void doDispose() {
    // empty
  }

  @Override
  protected void doConnect() throws Exception {
    // empty
  }

  @Override
  protected void doDisconnect() throws Exception {
    // empty
  }

  @Override
  protected void doStart() {
    // empty
  }

  @Override
  protected void doStop() {
    // empty
  }
}
