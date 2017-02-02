/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.validator;

import static org.mule.extension.http.api.error.HttpError.RESPONSE_VALIDATION;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.api.error.HttpError;
import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Optional;

/**
 * Signals that an error occurred while validating a {@link Message}
 *
 * @since 4.0
 */
public class ResponseValidatorException extends ModuleException implements ErrorMessageAwareException {

  Message errorMessage;

  public ResponseValidatorException(String message, Optional<HttpError> error) {
    super(message, error.orElse(RESPONSE_VALIDATION));
  }

  public ResponseValidatorException(String message, Optional<HttpError> error, Result<Object, HttpResponseAttributes> result) {
    this(message, error);
    this.errorMessage = Message.builder()
        .payload(result.getOutput())
        .attributes(result.getAttributes().get())
        .mediaType(result.getMediaType().orElse(ANY))
        .build();
  }

  @Override
  public Message getErrorMessage() {
    return errorMessage;
  }

  @Override
  public Throwable getRootCause() {
    return this;
  }

}
