/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.validator;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;

/**
 * Configures error handling of the response.
 *
 * @since 4.0
 */
public interface ResponseValidator {

  /**
   * Validates whether a message should be accepted or not, failing in that case.
   *
   * @param muleMessage the message to validate
   * @param context the Mule Context for the current application.
   * @throws ResponseValidatorException if the message is not considered valid.
   */
  void validate(Message muleMessage, MuleContext context) throws ResponseValidatorException;

}
