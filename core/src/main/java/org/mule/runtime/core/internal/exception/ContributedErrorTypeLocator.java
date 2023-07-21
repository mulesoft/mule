/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
