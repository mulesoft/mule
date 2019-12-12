/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static java.lang.String.format;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;

import javax.inject.Inject;

/**
 * Processor capable of raising errors on demand, given a type and optionally a message.
 *
 * @since 4.0
 */
public final class RaiseErrorProcessor extends AbstractComponent implements Processor, Initialisable {

  private static final String ERROR_MESSAGE = "An error occurred.";

  private AttributeEvaluator descriptionEvaluator = new AttributeEvaluator(ERROR_MESSAGE, STRING);
  private String typeId;
  private ErrorType errorType;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private ExtendedExpressionManager expressionManager;

  @Override
  public void initialise() throws InitialisationException {
    if (isEmpty(typeId)) {
      throw new InitialisationException(createStaticMessage("type cannot be an empty string or null"), this);
    }

    ComponentIdentifier errorTypeComponentIdentifier = buildFromStringRepresentation(typeId);
    errorType = errorTypeRepository.lookupErrorType(errorTypeComponentIdentifier)
        .orElseThrow(() -> new InitialisationException(createStaticMessage(format("Could not find error '%s'.", typeId)), this));
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
