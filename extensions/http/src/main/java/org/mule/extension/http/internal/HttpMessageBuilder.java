/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.service.http.api.domain.ParameterMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class HttpMessageBuilder implements Initialisable {

  private String name;
  protected Multimap<HttpParamType, HttpParam> params = ArrayListMultimap.create();

  @Override
  public void initialise() throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(params.values());
  }

  public ParameterMap resolveParams(Event muleEvent, HttpParamType httpParamType, MuleContext muleContext) {
    Iterable<HttpParam> paramList = params.get(httpParamType);
    ParameterMap httpParams = new ParameterMap();

    for (HttpParam httpParam : paramList) {
      httpParam.resolve(httpParams, muleEvent, muleContext);
    }

    return httpParams;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addHeader(String headerName, String headerValue) {
    final HttpSingleParam httpSingleParam = new HttpSingleParam(HttpParamType.HEADER);
    httpSingleParam.setName(headerName);
    httpSingleParam.setValue(headerValue);
    this.params.put(HttpParamType.HEADER, httpSingleParam);
  }

}
