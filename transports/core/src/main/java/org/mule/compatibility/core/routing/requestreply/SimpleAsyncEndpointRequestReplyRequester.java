/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing.requestreply;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.routing.requestreply.SimpleAsyncRequestReplyRequester;

public class SimpleAsyncEndpointRequestReplyRequester extends SimpleAsyncRequestReplyRequester implements Startable, Stoppable {

  @Override
  protected String getReplyTo() {
    InboundEndpoint endpoint = ((InboundEndpoint) replyMessageSource);
    return endpoint.getConnector().getCanonicalURI(endpoint.getEndpointURI());
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  @Override
  protected void verifyReplyMessageSource(MessageSource messageSource) {
    if (!(messageSource instanceof InboundEndpoint)) {
      throw new IllegalArgumentException("Only an InboundEndpoint reply MessageSource is supported with SimpleAsyncEndpointRequestReplyRequester");
    }
  }

}
