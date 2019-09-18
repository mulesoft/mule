/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import java.util.Objects;

import org.mule.runtime.api.message.ErrorType;

public final class WildcardErrorTypeMatcher implements ErrorTypeMatcher {

  public static String WILDCARD_TOKEN = "*";

  private final ErrorType errorType;

  private boolean identifierIsWildcard;

  private boolean namespaceIsWildcard;

  public WildcardErrorTypeMatcher(ErrorType errorType) {
    this.errorType = errorType;
    identifierIsWildcard = WILDCARD_TOKEN.equals(errorType.getIdentifier());
    namespaceIsWildcard = WILDCARD_TOKEN.equals(errorType.getNamespace());
  }

  @Override
  public boolean match(ErrorType errorType) {
    if (matchIdentifier(errorType) && matchNamespace(errorType) && matchParent(errorType)) {
      return true;
    }

    return isChild(errorType);
  }

  private boolean matchParent(ErrorType errorType) {
    return Objects.equals(this.errorType.getParentErrorType(), errorType.getParentErrorType());
  }

  private boolean matchNamespace(ErrorType errorType) {
    if (namespaceIsWildcard) {
      return true;
    }

    return Objects.equals(this.errorType.getNamespace(), errorType.getNamespace());
  }

  private boolean matchIdentifier(ErrorType errorType) {
    if (identifierIsWildcard) {
      return true;
    }

    return Objects.equals(this.errorType.getIdentifier(), errorType.getIdentifier());
  }

  private boolean isChild(ErrorType errorType) {
    ErrorType parentErrorType = errorType.getParentErrorType();
    return parentErrorType != null && this.match(parentErrorType);
  }
}
