/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.processor.simple;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.processor.simple.SimpleMessageProcessor;
import org.mule.runtime.core.util.AttributeEvaluator;

public class SetCorrelationIdTransformer extends SimpleMessageProcessor {

  private AttributeEvaluator correlationIdEvaluator;

  @Override
  public void initialise() throws InitialisationException {
    correlationIdEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public Event process(Event event) throws MuleException {
    return Event.builder(event).correlationId(correlationIdEvaluator.resolveValue(event).toString()).build();
  }


  public void setCorrelationId(String correlationId) {
    correlationIdEvaluator = new AttributeEvaluator(correlationId);
  }
}
