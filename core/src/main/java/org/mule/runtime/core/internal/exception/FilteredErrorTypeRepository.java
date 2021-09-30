/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class FilteredErrorTypeRepository implements ErrorTypeRepository {

  private final ErrorTypeRepository delegate;
  private final Set<String> filteredNamespaces;

  public FilteredErrorTypeRepository(ErrorTypeRepository delegate, Set<String> filteredNamespaces) {
    this.delegate = delegate;
    this.filteredNamespaces = filteredNamespaces;
  }

  @Override
  public ErrorType addErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ErrorType addInternalErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<ErrorType> lookupErrorType(ComponentIdentifier errorTypeComponentIdentifier) {
    if (filteredNamespaces.contains(errorTypeComponentIdentifier.getNamespace().toUpperCase())) {
      return delegate.lookupErrorType(errorTypeComponentIdentifier);
    } else {
      return empty();
    }
  }

  @Override
  public Optional<ErrorType> getErrorType(ComponentIdentifier errorTypeIdentifier) {
    if (filteredNamespaces.contains(errorTypeIdentifier.getNamespace().toUpperCase())) {
      return delegate.getErrorType(errorTypeIdentifier);
    } else {
      return empty();
    }
  }

  @Override
  public Collection<String> getErrorNamespaces() {
    return delegate.getErrorNamespaces().stream()
        .filter(ns -> filteredNamespaces.contains(ns.toUpperCase()))
        .collect(toList());
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


  @Override
  public Set<ErrorType> getErrorTypes() {
    return delegate.getErrorTypes()
        .stream()
        .filter(err -> filteredNamespaces.contains(err.getNamespace().toUpperCase()))
        .collect(toSet());
  }

  @Override
  public Set<ErrorType> getInternalErrorTypes() {
    return delegate.getInternalErrorTypes()
        .stream()
        .filter(err -> filteredNamespaces.contains(err.getNamespace().toUpperCase()))
        .collect(toSet());
  }

}
