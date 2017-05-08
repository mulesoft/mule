/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.expression.ExpressionConfig;

import java.util.List;

/**
 * Splits a message using the expression provided invoking the next message processor one for each split part.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www.eaipatterns.com/Sequencer.html</a>
 */
public class ExpressionSplitter extends AbstractSplitter implements Initialisable {

  protected ExpressionConfig config = new ExpressionConfig();
  private ExpressionSplitterStrategy expressionSplitterStrategy;

  public ExpressionSplitter() {
    // Used by spring
  }

  public ExpressionSplitter(ExpressionConfig config) {
    this.config = config;
  }

  @Override
  public void initialise() throws InitialisationException {
    config.validate();
    expressionSplitterStrategy = new ExpressionSplitterStrategy(muleContext.getExpressionManager());
  }

  @Override
  protected List<?> splitMessage(Event event) {
    return expressionSplitterStrategy.splitMessage(event, config.getFullExpression());
  }

  public String getExpression() {
    return config.getExpression();
  }

  public void setExpression(String expression) {
    this.config.setExpression(expression);
  }

}
