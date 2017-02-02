/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal;

import org.mule.runtime.core.api.MuleContext;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.runtime.core.api.Event;

public abstract class HttpParam {

  private HttpParamType type;

  public HttpParam(HttpParamType type) {
    this.type = type;
  }

  public HttpParamType getType() {
    return type;
  }

  public abstract void resolve(ParameterMap parameterMap, Event muleEvent, MuleContext muleContext);
}
