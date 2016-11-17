/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.validator;


import static java.lang.String.format;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;

/**
 * Response validator that allows specifying which status codes will be considered as successful. Other status codes in the
 * response will cause the component to throw an exception.
 *
 * @since 4.0
 */
public class SuccessStatusCodeValidator extends RangeStatusCodeValidator {

  /**
   * An status code validator that allows any status code.
   */
  public static SuccessStatusCodeValidator NULL_VALIDATOR = new SuccessStatusCodeValidator("0..599");

  public SuccessStatusCodeValidator() {}

  public SuccessStatusCodeValidator(String values) {
    setValues(values);
  }

  @Override
  public void validate(Message responseMessage, MuleContext context) throws ResponseValidatorException {
    int status = ((HttpResponseAttributes) responseMessage.getAttributes()).getStatusCode();

    if (!belongs(status)) {
      throw new ResponseValidatorException(format("Response code %d mapped as failure", status));
    }
  }

}
