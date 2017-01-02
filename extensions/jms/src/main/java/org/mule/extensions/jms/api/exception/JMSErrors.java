/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import static java.util.Optional.ofNullable;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

/**
 * Errors for the JMS extension
 * 
 * @since 4.0
 */
public enum JmsErrors implements ErrorTypeDefinition<JmsErrors> {

  PUBLISHING, CONSUMING, ILLEGAL_BODY(PUBLISHING), ACK(CONSUMING), TIMEOUT(CONSUMING), DESTINATION_NOT_FOUND;

  private ErrorTypeDefinition<?> parentErrortype;

  JmsErrors(ErrorTypeDefinition parentErrorType) {
    this.parentErrortype = parentErrorType;
  }

  JmsErrors() {}

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parentErrortype);
  }
}
