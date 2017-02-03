/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.functional;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.exception.TypedException;

import javax.inject.Inject;

/**
 * Processor that throws the specified exception. Can be refactored to throw errors later on
 *
 * @since 4.0
 */
public class ThrowProcessor implements Processor {

  private static final String EXCEPTION_ERROR = "If an error is not provided, the provided exception must be a TypedException.";

  private Class<? extends Throwable> exception;
  private String error;

  @Inject
  private MuleContext muleContext;

  @Override
  public Event process(Event event) throws MuleException {
    try {
      Throwable instantiatedException = exception.newInstance();
      if (error != null) {
        ErrorTypeRepository errorTypeRepository = muleContext.getErrorTypeRepository();
        ErrorType errorType = errorTypeRepository.lookupErrorType(buildFromStringRepresentation(error))
            .orElseThrow(() -> new DefaultMuleException(String.format("Could not find error: %s", error)));
        throw new TypedException(instantiatedException, errorType);
      } else {
        checkArgument(instantiatedException instanceof TypedException, EXCEPTION_ERROR);
        throw (TypedException) instantiatedException;
      }
    } catch (InstantiationException | IllegalAccessException e) {
      throw new DefaultMuleException(String.format("Failed to instantiate exception class %s", exception.getSimpleName()));
    }
  }

  public void setException(Class<? extends Throwable> exception) {
    this.exception = exception;
  }

  public void setError(String error) {
    this.error = error;
  }

}
