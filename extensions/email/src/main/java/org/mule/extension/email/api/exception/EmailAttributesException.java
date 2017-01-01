/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.api.exception.EmailErrors.ATTRIBUTES;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * @since 4.0
 */
public class EmailAttributesException extends ModuleException {

  public EmailAttributesException(String message, Exception exception) {
    super(exception, ATTRIBUTES, message);
  }

}
