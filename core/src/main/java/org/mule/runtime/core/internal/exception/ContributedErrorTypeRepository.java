/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;

import java.util.Collection;
import java.util.Optional;


public class ContributedErrorTypeRepository implements ErrorTypeRepository {

  private ErrorTypeRepository delegate;

  @Override
  public ErrorType addErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    return delegate.addErrorType(errorTypeIdentifier, parentErrorType);
  }

  @Override
  public ErrorType addInternalErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    return delegate.addInternalErrorType(errorTypeIdentifier, parentErrorType);
  }

  @Override
  public Optional<ErrorType> lookupErrorType(ComponentIdentifier errorTypeComponentIdentifier) {
    return delegate.lookupErrorType(errorTypeComponentIdentifier);
  }

  @Override
  public Optional<ErrorType> getErrorType(ComponentIdentifier errorTypeIdentifier) {
    return delegate.getErrorType(errorTypeIdentifier);
  }

  @Override
  public Collection<String> getErrorNamespaces() {
    return delegate.getErrorNamespaces();
  }

  @Override
  public ErrorType getAnyErrorType() {
    return delegate.getAnyErrorType();
  }

  @Override
  public ErrorType getSourceErrorType() {
    return delegate.getSourceErrorType();
  }

  @Override
  public ErrorType getSourceResponseErrorType() {
    return delegate.getSourceResponseErrorType();
  }

  @Override
  public ErrorType getCriticalErrorType() {
    return delegate.getCriticalErrorType();
  }

  public void setDelegate(ErrorTypeRepository delegate) {
    this.delegate = delegate;
  }

  public ErrorTypeRepository getDelegate() {
    return delegate;
  }
}
