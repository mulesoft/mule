/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;

import java.util.Collection;
import java.util.Optional;

/**
 * {@link ErrorTypeRepository} implementation which composes two {@link ErrorTypeRepository} in a hierarchy manner.
 * <p>
 * If the this repository can't process a request with the {@link CompositeErrorTypeRepository#childErrorTypeRepository},
 * it will fallback to the {@link CompositeErrorTypeRepository#parentErrorTypeRepository}
 *
 * @since 4.0
 */
public final class CompositeErrorTypeRepository implements ErrorTypeRepository {

  private final ErrorTypeRepository childErrorTypeRepository;
  private final ErrorTypeRepository parentErrorTypeRepository;

  /**
   * Creates a new {@link CompositeErrorTypeRepository} instance
   *
   * @param childErrorTypeRepository  {@link ErrorTypeRepository} considered as the main one
   * @param parentErrorTypeRepository {@link ErrorTypeRepository} considered as the secondary one, is a request
   *                                  can't be handled by the main {@link ErrorTypeRepository}, this one will be used
   */
  public CompositeErrorTypeRepository(ErrorTypeRepository childErrorTypeRepository,
                                      ErrorTypeRepository parentErrorTypeRepository) {
    this.childErrorTypeRepository = childErrorTypeRepository;
    this.parentErrorTypeRepository = parentErrorTypeRepository;
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
    if (!errorType.isPresent()) {
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
    if (!errorType.isPresent()) {
      errorType = parentErrorTypeRepository.getErrorType(errorTypeIdentifier);
    }
    return errorType;
  }

  @Override
  public Collection<String> getErrorNamespaces() {
    return concat(parentErrorTypeRepository.getErrorNamespaces().stream(),
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
