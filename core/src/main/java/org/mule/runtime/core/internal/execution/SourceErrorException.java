/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;

import java.util.Optional;

/**
 * Represents an error that happens when a Source is processing the response of a flow.
 * 
 * @since 4.0
 */
public final class SourceErrorException extends MuleRuntimeException {

  private static final long serialVersionUID = 160215774280116876L;

  private CoreEvent event;
  private ErrorType errorType;
  private MessagingException originalCause;

  public SourceErrorException(CoreEvent event, ErrorType errorType, Throwable cause) {
    super(cause);
    this.event = event;
    this.errorType = errorType;
    this.originalCause = null;
  }

  public SourceErrorException(CoreEvent event, ErrorType errorType, Throwable cause, MessagingException originalCause) {
    super(cause);
    this.event = event;
    this.errorType = errorType;
    this.originalCause = originalCause;
  }

  public CoreEvent getEvent() {
    return event;
  }

  public ErrorType getErrorType() {
    return errorType;
  }

  public Optional<MessagingException> getOriginalCause() {
    return ofNullable(originalCause);
  }

  public MessagingException toMessagingException() {
    return new MessagingException(CoreEvent.builder(getEvent())
        .error(ErrorBuilder.builder(getCause())
            .errorType(getErrorType())
            .build())
        .build(), getCause());
  }

}
