/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.util.Optional.empty;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.Preconditions.checkState;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.map.HashedMap;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.config.ComponentIdentifier;

/**
 * Locator for error types.
 *
 * The locator is responsible for getting the error type of any exception.
 *
 * An exception can be a general exception or an exception thrown by a configured component.
 *
 * There's a default mapping that will be used when there's no specific mapping between a component and the exception type thrown.
 *
 * To create an {@code ErrorTypeLocator} you must use the {@code Builder}. An instance of the builder can be created using the
 * static method {@code #builder}.
 * 
 * @since 4.0
 */
public class ErrorTypeLocator {

  private final ErrorTypeRepository errorTypeRepository;
  private ExceptionMapper defaultExceptionMapper;
  private Map<ComponentIdentifier, ExceptionMapper> componentExceptionMappers;

  private ErrorTypeLocator(ExceptionMapper defaultExceptionMapper,
                           Map<ComponentIdentifier, ExceptionMapper> componentExceptionMappers,
                           ErrorTypeRepository errorTypeRepository) {
    this.defaultExceptionMapper = defaultExceptionMapper;
    this.componentExceptionMappers = componentExceptionMappers;
    this.errorTypeRepository = errorTypeRepository;
  }

  /**
   * Gets the {@code ErrorType} instance for ANY error type.
   * 
   * @return the ANY error type
   */
  public ErrorType getAnyErrorType() {
    return errorTypeRepository.getAnyErrorType();
  }

  /**
   * Finds the {@code ErrorType} related to the provided {@code exception} based on the general mapping rules of the runtime.
   * 
   * @param exception the exception related to the error type
   * @return the error type related to the exception. If there's no mapping then the error type related to UNKNOWN will be
   *         returned.
   */
  public ErrorType lookupErrorType(Exception exception) {
    return defaultExceptionMapper.resolveErrorType(exception).get();
  }

  /**
   * Finds the {@code ErrorType} related to a component defined by the {@link ComponentIdentifier} based on the exception thrown
   * by the component and the mappings configured in the {@code ErrorTypeLocator}.
   * 
   * If no mapping is available then the {@link #getAnyErrorType()} rules applies.
   * 
   * @param componentIdentifier the identifier of the component that throw the exception.
   * @param exception the exception thrown by the component.
   * @return the error type realted to the exception based on the component mappings. If there's no mapping then the error type
   *         related to UNKNOWN will be returned.
   */
  public ErrorType lookupComponentErrorType(ComponentIdentifier componentIdentifier, Exception exception) {
    ExceptionMapper exceptionMapper = componentExceptionMappers.get(componentIdentifier);
    Optional<ErrorType> errorType = empty();
    if (exceptionMapper != null) {
      errorType = exceptionMapper.resolveErrorType(exception);
    }
    return errorType.orElseGet(() -> defaultExceptionMapper.resolveErrorType(exception).get());
  }

  /**
   * Builder for creating instances of {@link ErrorTypeLocator}.
   * 
   * @param errorTypeRepository repository of error types.
   * @return a builder for creating an {@link ErrorTypeLocator}
   */
  public static Builder builder(ErrorTypeRepository errorTypeRepository) {
    return new Builder(errorTypeRepository);
  }

  /**
   * Builder for {@link ErrorTypeLocator}
   * 
   * @since 4.0
   */
  public static class Builder {

    private final ErrorTypeRepository errorTypeRepository;

    /**
     * Creates a builder instance.
     * 
     * @param errorTypeRepository error type repository used to locate {@link ErrorType} instances.
     */
    public Builder(ErrorTypeRepository errorTypeRepository) {
      checkArgument(errorTypeRepository != null, "error type repository cannot be null");
      this.errorTypeRepository = errorTypeRepository;
    }

    private ExceptionMapper defaultExceptionMapper;
    private Map<ComponentIdentifier, ExceptionMapper> componentExceptionMappers = new HashedMap();

    /**
     * Sets the default exception mapper to use when a component doesn't define a mapping for an exception type.
     * 
     * @param exceptionMapper default exception mapper.
     * @return {@code this} builder.
     */
    public Builder defaultExceptionMapper(ExceptionMapper exceptionMapper) {
      this.defaultExceptionMapper = exceptionMapper;
      return this;
    }

    /**
     * Adds an {@link ExceptionMapper} for a particular component identified by the componentIdentifier.
     * 
     * @param componentIdentifier identifier of a component.
     * @param exceptionMapper exception mapper for the component.
     * @return {@code this} builder.
     */
    public Builder addComponentExceptionMapper(ComponentIdentifier componentIdentifier, ExceptionMapper exceptionMapper) {
      this.componentExceptionMappers.put(componentIdentifier, exceptionMapper);
      return this;
    }

    /**
     * Builds an {@link ErrorTypeLocator} instance with the provided configuration.
     * 
     * @return an {@link ErrorTypeLocator} instance.
     */
    public ErrorTypeLocator build() {
      checkState(defaultExceptionMapper != null, "default exception mapper cannot not be null");
      checkState(componentExceptionMappers != null, "component exception mappers cannot not be null");
      return new ErrorTypeLocator(defaultExceptionMapper, componentExceptionMappers, errorTypeRepository);
    }
  }
}
