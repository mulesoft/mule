/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.extension.http.api.HttpConstants.RequestProperties.HTTP_LISTENER_PATH;
import static org.mule.extension.http.api.HttpConstants.RequestProperties.HTTP_REQUEST_PATH_PROPERTY;
import static org.mule.extension.http.api.HttpConstants.RequestProperties.HTTP_REQUEST_URI;
import static org.mule.extension.http.api.HttpConstants.RequestProperties.HTTP_SCHEME;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.StringUtils;

public class HttpRequestPropertyManager {

  public String getRequestPath(InternalMessage message) {
    String requestUri = message.getInboundProperty(HTTP_REQUEST_URI);
    if (requestUri == null && message.getAttributes() instanceof HttpRequestAttributes) {
      requestUri = ((HttpRequestAttributes) message.getAttributes()).getRequestUri();
    }
    return requestUri != null ? requestUri : EMPTY;
  }

  public String getScheme(Event event) {
    String scheme = event.getMessage().getInboundProperty(HTTP_SCHEME);
    if (scheme == null && event.getMessage().getAttributes() instanceof HttpRequestAttributes) {
      scheme = ((HttpRequestAttributes) event.getMessage().getAttributes()).getScheme();
    }
    return scheme;
  }

  public String getBasePath(InternalMessage message) {
    String listenerPath = message.getInboundProperty(HTTP_LISTENER_PATH);
    if (listenerPath == null && message.getAttributes() instanceof HttpRequestAttributes) {
      listenerPath = ((HttpRequestAttributes) message.getAttributes()).getListenerPath();
    }
    String requestPath = message.getInboundProperty(HTTP_REQUEST_PATH_PROPERTY);
    if (requestPath == null && message.getAttributes() instanceof HttpRequestAttributes) {
      requestPath = ((HttpRequestAttributes) message.getAttributes()).getRequestPath();
    }
    if (listenerPath.contains(requestPath)) {
      return requestPath;
    }
    int slashCount = StringUtils.countMatches(listenerPath, "/");
    int matchPrefixIndex = StringUtils.ordinalIndexOf(requestPath, "/", slashCount);
    return requestPath.substring(0, matchPrefixIndex);
  }
}
