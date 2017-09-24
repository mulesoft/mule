/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.IOException;

import javax.inject.Inject;

public class ExpressionLanguageComponent extends AbstractComponent implements Processor, Initialisable {

  @Inject
  private ExtendedExpressionManager expressionMgr;
  protected FlowConstruct flowConstruct;
  protected String expression;
  protected String expressionFile;

  @Override
  public void initialise() throws InitialisationException {
    if (expressionFile != null) {
      try {
        expression = getResourceAsString(expressionFile, getClass());
      } catch (IOException e) {
        throw new InitialisationException(e, this);
      }
    } else if (expression == null) {
      throw new InitialisationException(objectIsNull("expression"), this);
    }
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    expressionMgr.evaluate(expression, event, eventBuilder, getLocation());
    return eventBuilder.build();
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setExpressionFile(String expressionFile) {
    this.expressionFile = expressionFile;
  }
}
