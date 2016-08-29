/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.config.i18n.Message;

/**
 * The exception to be thrown by default when a validation fails. It's a pretty simple {@link MuleException} with the added
 * ability to provide the {@link ValidationResult} which failed.
 * <p/>
 * The exception message is set to match the one in {@link ValidationResult#getMessage()}
 *
 * @since 3.7.0
 */
public class ValidationException extends MessagingException {

  private final ValidationResult validationResult;

  /**
   * Creates a new instance for the given {@code validationResult}
   *
   * @param validationResult a failing {@link ValidationResult}
   * @param event {@link MuleEvent} on which validation failed
   */
  public ValidationException(ValidationResult validationResult, MuleEvent event) {
    super(createStaticMessage(validationResult.getMessage()), event);
    this.validationResult = validationResult;
  }

  /**
   * @return the {@link ValidationResult} which caused this exception
   */
  public ValidationResult getValidationResult() {
    return validationResult;
  }

  @Override
  protected String generateMessage(Message message, MuleContext context) {
    return message.getMessage();
  }
}
