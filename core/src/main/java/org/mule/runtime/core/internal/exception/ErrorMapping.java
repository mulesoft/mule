/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;

import javax.xml.namespace.QName;

/**
 * Represents a mapping between source and target {@link ErrorType}s, indicating that if
 * the first or a match for it is found then the other should be propagated instead.
 *
 * @since 4.0
 */
public class ErrorMapping {

  public static QName ANNOTATION_ERROR_MAPPINGS = new QName("operator", "errorMappings");

  private ErrorTypeMatcher sourceMatcher;
  private ErrorType target;

  public ErrorMapping(ErrorTypeMatcher sourceMatcher, ErrorType target) {
    this.sourceMatcher = sourceMatcher;
    this.target = target;
  }

  public boolean match(ErrorType errorType) {
    return sourceMatcher.match(errorType);
  }

  public ErrorType getTarget() {
    return target;
  }

}
