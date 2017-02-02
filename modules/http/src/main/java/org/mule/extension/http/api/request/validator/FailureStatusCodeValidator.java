/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.validator;

import static org.mule.extension.http.api.error.HttpError.getErrorByCode;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.extension.api.runtime.operation.Result;

/**
 * Response validator that allows specifying which status codes should be treated as failures. Responses with such status codes
 * will cause the component to throw an exception.
 *
 * @since 4.0
 */
public class FailureStatusCodeValidator extends RangeStatusCodeValidator {

  @Override
  public void validate(Result<Object, HttpResponseAttributes> result) throws ResponseValidatorException {
    int status = result.getAttributes().get().getStatusCode();

    if (belongs(status)) {
      throw new ResponseValidatorException(getExceptionMessage(status), getErrorByCode(status), result);
    }
  }

}
