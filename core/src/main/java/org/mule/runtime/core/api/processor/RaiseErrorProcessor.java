/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;

import javax.inject.Inject;

/**
 * Processor capable of raising errors on demand, given a type and optionally a message.
 *
 * @since 4.0
 */
public class RaiseErrorProcessor extends AbstractComponent implements Processor, Initialisable {

  private static final String ERROR_MESSAGE = "An error occurred.";

  private AttributeEvaluator descriptionEvaluator = new AttributeEvaluator(ERROR_MESSAGE, STRING);
  private String typeId;
  private ErrorType errorType;

  @Inject
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    errorType = muleContext.getErrorTypeRepository().lookupErrorType(buildFromStringRepresentation(typeId)).get();
    ExtendedExpressionManager expressionManager = muleContext.getExpressionManager();
    descriptionEvaluator.initialize(expressionManager);
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    String message = descriptionEvaluator.resolveValue(event);
    throw new TypedException(new DefaultMuleException(message), errorType);
  }

  public void setType(String type) {
    this.typeId = type;
  }

  public void setDescription(String description) {
    this.descriptionEvaluator = new AttributeEvaluator(description, STRING);
  }

}
