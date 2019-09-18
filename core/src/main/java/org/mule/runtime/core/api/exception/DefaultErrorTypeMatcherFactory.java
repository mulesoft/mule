/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher.WILDCARD_TOKEN;

import java.util.Objects;

import org.mule.runtime.api.message.ErrorType;

public class DefaultErrorTypeMatcherFactory implements ErrorTypeMatcherFactory {

  @Override
  public ErrorTypeMatcher create(ErrorType errorType) {
    if (doesErrorTypeContainWildcards(errorType)) {
      return new WildcardErrorTypeMatcher(errorType);
    } else {
      return new SingleErrorTypeMatcher(errorType);
    }
  }

  private static boolean doesErrorTypeContainWildcards(ErrorType errorType) {
    if (errorType == null) {
      return false;
    }

    if (Objects.equals(WILDCARD_TOKEN, errorType.getIdentifier())) {
      return true;
    }

    if (Objects.equals(WILDCARD_TOKEN, errorType.getNamespace())) {
      return true;
    }

    return false;
  }
}
