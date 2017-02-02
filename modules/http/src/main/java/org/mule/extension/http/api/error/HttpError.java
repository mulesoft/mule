/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.error;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;
import static org.mule.service.http.api.HttpConstants.HttpStatus.getStatusByCode;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.service.http.api.HttpConstants.HttpStatus;

import java.util.Optional;

/**
 * Represents an error that can happen in an HTTP operation.
 *
 * @since 4.0
 */
public enum HttpError implements ErrorTypeDefinition<HttpError> {

  PARSING,

  TIMEOUT,

  SECURITY(MuleErrors.SECURITY),

  TRANSFORMATION(MuleErrors.TRANSFORMATION),

  CONNECTIVITY(MuleErrors.CONNECTIVITY),

  RESPONSE_VALIDATION(ANY),

  BAD_REQUEST(RESPONSE_VALIDATION),

  UNAUTHORIZED(RESPONSE_VALIDATION),

  FORBIDDEN(RESPONSE_VALIDATION),

  NOT_FOUND(RESPONSE_VALIDATION),

  METHOD_NOT_ALLOWED(RESPONSE_VALIDATION),

  NOT_ACCEPTABLE(RESPONSE_VALIDATION),

  UNSUPPORTED_MEDIA_TYPE(RESPONSE_VALIDATION),

  TOO_MANY_REQUESTS(RESPONSE_VALIDATION),

  INTERNAL_SERVER_ERROR(RESPONSE_VALIDATION),

  SERVICE_UNAVAILABLE(RESPONSE_VALIDATION);

  private ErrorTypeDefinition<?> parentErrorType;

  HttpError() {}

  HttpError(ErrorTypeDefinition<?> parentErrorType) {
    this.parentErrorType = parentErrorType;
  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parentErrorType);
  }

  /**
   * Returns the {@link HttpError} corresponding to a given status code. A match is found if there's an {@link HttpError} with
   * the same name as the status code's corresponding {@link HttpStatus}.
   *
   * @param statusCode the HTTP status code to search for
   * @return an {@link Optional} with the
   */
  public static Optional<HttpError> getErrorByCode(int statusCode) {
    HttpError error = null;
    HttpStatus status = getStatusByCode(statusCode);
    if (status != null) {
      try {
        error = HttpError.valueOf(status.name());
      } catch (Throwable e) {
        // Do nothing
      }
    }
    return ofNullable(error);
  }

  /**
   * Returns an {@link HttpStatus} corresponding to a given {@link HttpError}. A match is found if there's an {@link HttpStatus}
   * with the same name as the {@link HttpError}.
   *
   * @param error the {@link HttpError} to match
   * @return
   */
  public static Optional<HttpStatus> getHttpStatus(HttpError error) {
    HttpStatus result = null;
    for (HttpStatus status : HttpStatus.values()) {
      if (error.name().equals(status.name())) {
        result = status;
      }
    }
    return ofNullable(result);
  }

}
