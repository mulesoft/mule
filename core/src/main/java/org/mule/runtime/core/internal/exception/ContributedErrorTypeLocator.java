/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

public class ContributedErrorTypeLocator extends ErrorTypeLocator {

  public ContributedErrorTypeLocator() {
    super(null, emptyMap(), null);
  }

  private ErrorTypeLocator delegate;

  @Override
  public ErrorType lookupErrorType(Throwable exception) {
    return delegate.lookupErrorType(exception);
  }

  @Override
  public ErrorType lookupErrorType(Class<? extends Throwable> type) {
    return delegate.lookupErrorType(type);
  }

  @Override
  public ErrorType lookupComponentErrorType(ComponentIdentifier componentIdentifier, Class<? extends Throwable> exception) {
    return delegate.lookupComponentErrorType(componentIdentifier, exception);
  }

  @Override
  public ErrorType lookupComponentErrorType(ComponentIdentifier componentIdentifier, Throwable exception) {
    return delegate.lookupComponentErrorType(componentIdentifier, exception);
  }

  public void setDelegate(ErrorTypeLocator delegate) {
    this.delegate = delegate;
  }

}
