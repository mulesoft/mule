/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;

public class ExpressionLanguageComponent extends AbstractAnnotatedObject
    implements Processor, MuleContextAware, FlowConstructAware, Initialisable {

  protected MuleContext muleContext;
  protected FlowConstruct flowConstruct;
  protected String expression;
  protected String expressionFile;

  @Override
  public void initialise() throws InitialisationException {
    if (expressionFile != null) {
      try {
        expression = IOUtils.getResourceAsString(expressionFile, getClass());
      } catch (IOException e) {
        throw new InitialisationException(e, this);
      }
    } else if (expression == null) {
      throw new InitialisationException(CoreMessages.objectIsNull("expression"), this);
    }
  }

  @Override
  public Event process(Event event) throws MuleException {
    Event.Builder eventBuilder = Event.builder(event);
    muleContext.getExpressionLanguage().evaluate(expression, event, eventBuilder, flowConstruct);
    return eventBuilder.build();
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setExpressionFile(String expressionFile) {
    this.expressionFile = expressionFile;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
