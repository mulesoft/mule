/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import static java.lang.String.format;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.util.attribute.AttributeEvaluator;

import jakarta.inject.Inject;

public abstract class AbstractRaiseErrorProcessor extends AbstractComponent implements Processor, Initialisable {

  private static final String DEFAULT_ERROR_MESSAGE = "An error occurred.";

  private AttributeEvaluator descriptionEvaluator = new AttributeEvaluator(DEFAULT_ERROR_MESSAGE, STRING);
  private String typeId;
  private ErrorType errorType;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  protected ExtendedExpressionManager expressionManager;

  @Override
  public void initialise() throws InitialisationException {
    // These validations are already covered by RaiseErrorTypeReferencesPresent and RaiseErrorTypeReferencesExist
    // So it should never reach this point since these situations must be caught eagerly by those validations
    if (isEmpty(typeId)) {
      throw new InitialisationException(createStaticMessage("type cannot be an empty string or null"), this);
    }

    ComponentIdentifier errorTypeComponentIdentifier = calculateErrorIdentifier(typeId);

    errorType = errorTypeRepository.lookupErrorType(errorTypeComponentIdentifier)
        .orElseThrow(() -> new InitialisationException(createStaticMessage(format("Could not find error '%s'.",
                                                                                  errorTypeComponentIdentifier)),
                                                       this));

    descriptionEvaluator.initialize(expressionManager);
  }

  protected abstract ComponentIdentifier calculateErrorIdentifier(String typeId);

  protected abstract TypedException getException(ErrorType type, String message, CoreEvent event);

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    String message = descriptionEvaluator.resolveValue(event);
    throw getException(errorType, message, event);
  }

  public void setType(String type) {
    this.typeId = type;
  }

  public void setDescription(String description) {
    this.descriptionEvaluator = new AttributeEvaluator(description, STRING);
  }
}
