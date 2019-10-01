/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import java.util.Objects;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.ErrorType;

public final class WildcardErrorTypeMatcher implements ErrorTypeMatcher {

  public static String WILDCARD_TOKEN = "*";

  private final ComponentIdentifier errorTypeIdentifier;

  private boolean nameIsWildcard;

  private boolean namespaceIsWildcard;

  public WildcardErrorTypeMatcher(ComponentIdentifier errorTypeIdentifier) {
    this.errorTypeIdentifier = errorTypeIdentifier;
    this.nameIsWildcard = WILDCARD_TOKEN.equals(errorTypeIdentifier.getName());
    this.namespaceIsWildcard = WILDCARD_TOKEN.equals(errorTypeIdentifier.getNamespace());
  }

  @Override
  public boolean match(ErrorType errorType) {
    if (matchIdentifier(errorType) && matchNamespace(errorType)) {
      return true;
    }

    // If the error to match is "*:ID", then also match children in any namespace.
    if (!nameIsWildcard && isChild(errorType)) {
      return true;
    }

    // If the error to match is "NS:*" and the namespace didn't match, then don't match children.
    return false;
  }

  private boolean matchNamespace(ErrorType errorType) {
    if (namespaceIsWildcard) {
      return true;
    }

    return Objects.equals(this.errorTypeIdentifier.getNamespace(), errorType.getNamespace());
  }

  private boolean matchIdentifier(ErrorType errorType) {
    if (nameIsWildcard) {
      return true;
    }

    return Objects.equals(this.errorTypeIdentifier.getName(), errorType.getIdentifier());
  }

  private boolean isChild(ErrorType errorType) {
    ErrorType parentErrorType = errorType.getParentErrorType();
    return parentErrorType != null && this.match(parentErrorType);
  }
}
