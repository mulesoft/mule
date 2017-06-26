/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.message.ErrorType;

import java.util.List;

public class DisjunctiveErrorTypeMatcher implements ErrorTypeMatcher {

  List<ErrorTypeMatcher> errorTypeMatchers;

  public DisjunctiveErrorTypeMatcher(List<ErrorTypeMatcher> errorTypeMatchers) {
    this.errorTypeMatchers = errorTypeMatchers;
  }

  @Override
  public boolean match(ErrorType errorType) {
    return errorTypeMatchers.stream().anyMatch(errorTypeMatcher -> errorTypeMatcher.match(errorType));
  }
}
