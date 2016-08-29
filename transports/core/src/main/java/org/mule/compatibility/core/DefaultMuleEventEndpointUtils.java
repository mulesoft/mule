/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core;

import static org.mule.runtime.core.api.config.MuleProperties.ENDPOINT_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METHOD_PROPERTY;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

public class DefaultMuleEventEndpointUtils {

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public static void populateFieldsFromInboundEndpoint(MuleEvent event, InboundEndpoint endpoint) {
    ((DefaultMuleEvent) event).setEndpointFields(endpoint.getEncoding(), endpoint.getExchangePattern(),
                                                 endpoint.getTransactionConfig().isTransacted());

    fillProperties(event, endpoint);
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  protected static void fillProperties(MuleEvent event, InboundEndpoint endpoint) {
    if (endpoint != null && endpoint.getProperties() != null) {
      for (Object name : endpoint.getProperties().keySet()) {
        String prop = (String) name;

        // don't overwrite property on the message
        if (!ignoreProperty(prop, event.getMessage())) {
          // inbound endpoint flowVariables are in the invocation scope
          Object value = endpoint.getProperties().get(prop);
          event.setFlowVariable(prop, value);
        }
      }
    }
  }

  /**
   * This method is used to determine if a property on the previous event should be ignored for the next event. This method is
   * here because we don't have proper scoped handling of meta data yet The rules are
   * <ol>
   * <li>If a property is already set on the current event don't overwrite with the previous event value
   * <li>If the property name appears in the ignoredPropertyOverrides list, then we always set it on the new event
   * </ol>
   *
   * @param key The name of the property to ignore
   * @param message
   * @return true if the property should be ignored, false otherwise
   */
  private static boolean ignoreProperty(String key, MuleMessage message) {
    if (key == null || key.startsWith(ENDPOINT_PROPERTY_PREFIX)) {
      return true;
    }

    for (String ignoredPropertyOverride : new String[] {MULE_METHOD_PROPERTY}) {
      if (key.equals(ignoredPropertyOverride)) {
        return false;
      }
    }

    return null != message.getOutboundProperty(key);
  }

}
