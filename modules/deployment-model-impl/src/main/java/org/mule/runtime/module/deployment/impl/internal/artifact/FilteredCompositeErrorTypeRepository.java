/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;

import java.util.Collection;
import java.util.Optional;

final class FilteredCompositeErrorTypeRepository implements ErrorTypeRepository {

  private final ErrorTypeRepository childErrorTypeRepository;
  private final ErrorTypeRepository parentErrorTypeRepository;
  private final String namespaceFromParent;

  /**
   * Creates a new {@link FilteredCompositeErrorTypeRepository} instance
   *
   * @param childErrorTypeRepository {@link ErrorTypeRepository} considered as the main one
   * @param parentErrorTypeRepository {@link ErrorTypeRepository} considered as the secondary one, is a request can't be handled
   *        by the main {@link ErrorTypeRepository}, this one will be used
   * @param namespaceFromParent only errors with this namespace will be seen form the {@code parentErrorTypeRepository}
   */
  public FilteredCompositeErrorTypeRepository(ErrorTypeRepository childErrorTypeRepository,
                                              ErrorTypeRepository parentErrorTypeRepository,
                                              String namespaceFromParent) {
    this.childErrorTypeRepository = childErrorTypeRepository;
    this.parentErrorTypeRepository = parentErrorTypeRepository;
    this.namespaceFromParent = namespaceFromParent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType addErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    return childErrorTypeRepository.addErrorType(errorTypeIdentifier, parentErrorType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType addInternalErrorType(ComponentIdentifier errorTypeIdentifier, ErrorType parentErrorType) {
    return childErrorTypeRepository.addInternalErrorType(errorTypeIdentifier, parentErrorType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ErrorType> lookupErrorType(ComponentIdentifier errorTypeComponentIdentifier) {
    Optional<ErrorType> errorType = childErrorTypeRepository.lookupErrorType(errorTypeComponentIdentifier);
    if (!errorType.isPresent() && namespaceFromParent.equals(errorTypeComponentIdentifier.getNamespace())) {
      errorType = parentErrorTypeRepository.lookupErrorType(errorTypeComponentIdentifier);
    }
    return errorType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ErrorType> getErrorType(ComponentIdentifier errorTypeIdentifier) {
    Optional<ErrorType> errorType = childErrorTypeRepository.getErrorType(errorTypeIdentifier);
    if (!errorType.isPresent() && namespaceFromParent.equals(errorTypeIdentifier.getNamespace())) {
      errorType = parentErrorTypeRepository.getErrorType(errorTypeIdentifier);
    }
    return errorType;
  }

  @Override
  public Collection<String> getErrorNamespaces() {
    return concat(parentErrorTypeRepository.getErrorNamespaces().stream()
        .filter(namespaceFromParent::equals),
                  childErrorTypeRepository.getErrorNamespaces().stream()).collect(toSet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType getAnyErrorType() {
    return childErrorTypeRepository.getAnyErrorType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType getSourceErrorType() {
    return childErrorTypeRepository.getSourceErrorType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType getSourceResponseErrorType() {
    return childErrorTypeRepository.getSourceResponseErrorType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ErrorType getCriticalErrorType() {
    return childErrorTypeRepository.getCriticalErrorType();
  }
}
