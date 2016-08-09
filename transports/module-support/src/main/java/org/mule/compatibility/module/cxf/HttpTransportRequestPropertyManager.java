/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.cxf.HttpRequestPropertyManager;
import org.mule.runtime.module.http.api.HttpConstants;

public class HttpTransportRequestPropertyManager extends HttpRequestPropertyManager {

  @Override
  public String getRequestPath(MuleMessage message) {
    String requestPath = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_REQUEST_PROPERTY, StringUtils.EMPTY);
    if (requestPath.equals(StringUtils.EMPTY)) {
      requestPath = super.getRequestPath(message);
    }
    return requestPath;
  }

  @Override
  public String getScheme(MuleEvent event) {
    String scheme = event.getMessageSourceURI().getScheme();
    if (scheme == null) {
      scheme = super.getScheme(event);
    }
    return scheme;
  }

  @Override
  public String getBasePath(MuleMessage message) {
    String basePath = message.getInboundProperty(HttpConstants.RequestProperties.HTTP_CONTEXT_PATH_PROPERTY);
    if (basePath == null) {
      basePath = super.getBasePath(message);
    }
    return basePath;
  }
}
