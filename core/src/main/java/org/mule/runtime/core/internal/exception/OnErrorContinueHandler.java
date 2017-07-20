/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.lang.String.format;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.ErrorTypeRepository;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.internal.message.InternalMessage;

/**
 * Handler that will consume errors and finally commit transactions. Replaces the catch-exception-strategy from Mule 3.
 *
 * @since 4.0
 */
public class OnErrorContinueHandler extends TemplateOnErrorHandler {

  private ErrorTypeMatcher sourceErrorMatcher;


  public OnErrorContinueHandler() {
    setHandleException(true);
  }

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);

    ErrorTypeRepository errorTypeRepository = muleContext.getErrorTypeRepository();
    sourceErrorMatcher = new SingleErrorTypeMatcher(errorTypeRepository.getSourceResponseErrorType());

    if (errorType != null) {
      String[] errors = errorType.split(",");
      for (String error : errors) {
        // Since the partial initialisation was successful, we know this error ids are safe
        String sanitizedError = error.trim();
        ErrorType errorType = errorTypeRepository.lookupErrorType(buildFromStringRepresentation(sanitizedError)).get();
        if (sourceErrorMatcher.match(errorType)) {
          throw new InitialisationException(getInitialisationError(sanitizedError), this);
        }
      }
    }

  }

  private I18nMessage getInitialisationError(String type) {
    return createStaticMessage(format("Source errors are not allowed in 'on-error-continue' handlers. Offending type is '%s'.",
                                      type));
  }

  @Override
  protected Event nullifyExceptionPayloadIfRequired(Event event) {
    return Event.builder(event)
        .error(event.getError().orElse(null))
        .message(InternalMessage.builder(event.getMessage()).exceptionPayload(null).build())
        .build();
  }

  @Override
  public boolean accept(Event event) {
    return !sourceError(event) && super.accept(event);
  }

  private boolean sourceError(Event event) {
    return event.getError().filter(error -> sourceErrorMatcher.match(event.getError().get().getErrorType())).isPresent();
  }
}
