/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.validation;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;

/**
 * Exception to signal that a given json did not pass validation against a json schema
 *
 * @since 3.6.0
 */
public class JsonSchemaValidationException extends MuleException {

  /**
   * A {@link String} representation of the json
   */
  private final String invalidJson;

  public JsonSchemaValidationException(String validationError, String invalidJson) {
    super(I18nMessageFactory.createStaticMessage(validationError));
    this.invalidJson = invalidJson;
  }

  public JsonSchemaValidationException(String validationError, String invalidJson, Exception exception) {
    super(I18nMessageFactory.createStaticMessage(validationError), exception);
    this.invalidJson = invalidJson;
  }

  /**
   * Returns a {@link String} representation of the failing json
   *
   * @return a {@link String}
   */
  public String getInvalidJson() {
    return invalidJson;
  }
}
