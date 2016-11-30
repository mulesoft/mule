/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.runtime.core.util.AttributeEvaluator;

public class HttpSingleParam extends HttpParam implements Initialisable, MuleContextAware {

  private AttributeEvaluator name;
  private AttributeEvaluator value;

  private MuleContext muleContext;

  public HttpSingleParam(HttpParamType type) {
    super(type);
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public void initialise() throws InitialisationException {
    name.initialize(muleContext.getExpressionManager());
    value.initialize(muleContext.getExpressionManager());
  }

  @Override
  public void resolve(ParameterMap parameterMap, Event muleEvent, MuleContext muleContext) {
    parameterMap.put(name.resolveStringValue(muleEvent), value.resolveStringValue(muleEvent));
  }

  public void setName(String name) {
    this.name = new AttributeEvaluator(name);
  }

  public void setValue(String value) {
    this.value = new AttributeEvaluator(value);
  }

}
