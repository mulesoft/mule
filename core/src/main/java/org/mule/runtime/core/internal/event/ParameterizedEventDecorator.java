/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.api.el.BindingContextUtils.addParametersToBuilder;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;

import java.util.Map;

public class ParameterizedEventDecorator extends BaseEventDecorator {

  public static CoreEvent parameterized(CoreEvent event, Map<String, Object> parameters) {
    if (event instanceof ParameterizedEventDecorator) {
      event = ((ParameterizedEventDecorator) event).getEvent();
    }

    return event instanceof InternalEvent
        ? new ParameterizedEventDecorator((InternalEvent) event, parameters)
        //TODO: discuss with Rodro. What to do here? is this event possible?
        : event;
  }

  public static CoreEvent deparameterize(CoreEvent event) {
    while (event instanceof ParameterizedEventDecorator) {
      event = ((ParameterizedEventDecorator) event).getEvent();
    }

    return event;
  }


  private final Map<String, Object> parameters;


  private ParameterizedEventDecorator(InternalEvent event, Map<String, Object> parameters) {
    super(event);
    this.parameters = parameters;
  }

  @Override
  protected BindingContext.Builder doCreateBindingContextBuilder() {
    return addParametersToBuilder(super.doCreateBindingContextBuilder().addBinding("params", parameters));
  }

}
