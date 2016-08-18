/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.DefaultMuleEvent;

public class DefaultMuleEventEndpointUtils {

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public static void populateFieldsFromInboundEndpoint(DefaultMuleEvent event, InboundEndpoint endpoint) {
    event.setEndpointFields(endpoint.getEncoding(), endpoint.getExchangePattern(),
                            endpoint.getName(), endpoint.getEndpointURI().getUri(),
                            endpoint.getTransactionConfig().isTransacted());

    fillProperties(event, endpoint);
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  protected static void fillProperties(DefaultMuleEvent event, InboundEndpoint endpoint) {
    if (endpoint != null && endpoint.getProperties() != null) {
      for (Object name : endpoint.getProperties().keySet()) {
        String prop = (String) name;

        // don't overwrite property on the message
        if (!event.ignoreProperty(prop)) {
          // inbound endpoint flowVariables are in the invocation scope
          Object value = endpoint.getProperties().get(prop);
          event.setFlowVariable(prop, value);
        }
      }
    }
  }
}
