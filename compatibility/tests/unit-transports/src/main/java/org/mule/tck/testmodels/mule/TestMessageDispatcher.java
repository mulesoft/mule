/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.routing.RoutingException;

public class TestMessageDispatcher extends AbstractMessageDispatcher {

  public TestMessageDispatcher(final OutboundEndpoint endpoint) {
    super(endpoint);
  }

  @Override
  protected void doDispatch(Event event) throws Exception {
    if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail")) {
      throw new RoutingException((OutboundEndpoint) endpoint);
    }
  }

  @Override
  protected InternalMessage doSend(Event event) throws Exception {
    if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail")) {
      throw new RoutingException((OutboundEndpoint) endpoint);
    }
    return event.getMessage();
  }

  @Override
  protected void doConnect() throws Exception {
    // no op
  }

  @Override
  protected void doDisconnect() throws Exception {
    // no op
  }

  @Override
  protected void doStart() {
    // no op
  }

  @Override
  protected void doStop() {
    // no op
  }

}
