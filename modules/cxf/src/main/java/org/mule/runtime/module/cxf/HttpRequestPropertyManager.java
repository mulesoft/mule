/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.api.HttpConstants;

public class HttpRequestPropertyManager {

  public String getRequestPath(MuleMessage message) {
    return message.getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_URI, StringUtils.EMPTY);
  }

  public String getScheme(MuleEvent event) {
    return event.getMessage().getInboundProperty(HttpConstants.RequestProperties.HTTP_SCHEME);
  }

  public String getBasePath(MuleMessage message) {
    String listenerPath = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_LISTENER_PATH);
    String requestPath = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY);
    if (listenerPath.contains(requestPath)) {
      return requestPath;
    }
    int slashCount = StringUtils.countMatches(listenerPath, "/");
    int matchPrefixIndex = StringUtils.ordinalIndexOf(requestPath, "/", slashCount);
    return requestPath.substring(0, matchPrefixIndex);
  }
}
