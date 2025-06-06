/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.String.format;
import static org.mule.runtime.api.message.error.matcher.ErrorTypeMatcherUtils.createErrorTypeMatcher;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;

import java.util.Optional;

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
  protected void doInitialise() throws InitialisationException {
    super.doInitialise();

    sourceErrorMatcher = createErrorTypeMatcher(getErrorTypeRepository().getSourceResponseErrorType());

    if (errorType != null) {
      String[] errors = errorType.split(",");
      for (String error : errors) {
        String sanitizedError = error.trim();
        ComponentIdentifier errorTypeIdentifier = buildFromStringRepresentation(sanitizedError);

        Optional<ErrorType> errorType = getErrorTypeRepository().lookupErrorType(errorTypeIdentifier);
        if (errorType.isPresent()) {
          if (sourceErrorMatcher.match(errorType.get())) {
            throw new InitialisationException(getInitialisationError(sanitizedError), this);
          }
        }
      }
    } else if (!when.isPresent()) {
      // No error type and no expression, force ANY matcher
      errorTypeMatcher = createErrorTypeMatcher(getErrorTypeRepository().getAnyErrorType());
    }

  }

  private I18nMessage getInitialisationError(String type) {
    return createStaticMessage(format("Source errors are not allowed in 'on-error-continue' handlers. Offending type is '%s'.",
                                      type));
  }

  @Override
  public boolean acceptsAll() {
    // An on-error-continue cannot handle source response errors
    return false;
  }

  @Override
  protected CoreEvent nullifyExceptionPayloadIfRequired(CoreEvent event) {
    return CoreEvent.builder(event).error(null).build();
  }

  @Override
  public boolean accept(CoreEvent event) {
    return !sourceError(event) && super.accept(event);
  }

  /**
   * {@inheritDoc}
   *
   * @param buildFor
   */
  @Override
  public TemplateOnErrorHandler duplicateFor(ComponentLocation buildFor) {
    OnErrorContinueHandler cpy = new OnErrorContinueHandler();
    cpy.setFlowLocation(buildFor);
    when.ifPresent(expr -> cpy.setWhen(expr));
    cpy.setHandleException(this.handleException);
    cpy.setErrorType(this.errorType);
    cpy.setMessageProcessors(this.getMessageProcessors());
    cpy.setExceptionListener(this.getExceptionListener());
    cpy.setAnnotations(this.getAnnotations());
    return cpy;
  }

  private boolean sourceError(CoreEvent event) {
    final Optional<Error> error = event.getError();
    return error.isPresent() && sourceErrorMatcher.match(error.get().getErrorType());
  }
}
