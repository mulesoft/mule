/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request;

import org.mule.runtime.core.api.MuleContext;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.module.http.internal.HttpMessageBuilder;
import org.mule.runtime.module.http.internal.HttpParamType;

import java.util.List;

public class HttpRequesterRequestBuilder extends HttpMessageBuilder {

  public ParameterMap getQueryParams(Event event, MuleContext muleContext) {
    return resolveParams(event, HttpParamType.QUERY_PARAM, muleContext);
  }

  public ParameterMap getHeaders(Event event, MuleContext muleContext) {
    return resolveParams(event, HttpParamType.HEADER, muleContext);
  }

  public String replaceUriParams(String path, Event event, MuleContext muleContext) {
    ParameterMap uriParamMap = resolveParams(event, HttpParamType.URI_PARAM, muleContext);

    for (String uriParamName : uriParamMap.keySet()) {
      // If more than one value has been defined for a URI param, use the last one (to allow overrides)

      List<String> uriParamValues = uriParamMap.getAll(uriParamName);
      String uriParamValue = uriParamValues.get(uriParamValues.size() - 1);

      if (uriParamValue == null) {
        throw new NullPointerException(String.format("Expression {%s} evaluated to null.", uriParamName));
      }

      path = path.replaceAll(String.format("\\{%s\\}", uriParamName), uriParamValue);
    }
    return path;
  }

}
