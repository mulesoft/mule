/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.internal.exception.ExceptionMapping;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Defines a set of mappings between exception types and error types. Such configuration is later used to resolve the
 * {@link ErrorType} that must be used when an {@link Exception} is thrown by a mule component.
 * 
 * To create instances of {@link ExceptionMapper} you must use the {@link #builder()} method.
 * 
 * @since 4.0
 */
public class ExceptionMapper {

  private Set<ExceptionMapping> exceptionMappings = new TreeSet<>();

  /**
   * Creates a new {@link ExceptionMapper}
   * 
   * @param exceptionMappings set of mappings between exceptions and error types/
   */
  private ExceptionMapper(Set<ExceptionMapping> exceptionMappings) {
    this.exceptionMappings = exceptionMappings;
  }

  /**
   * Using the set of {@link ExceptionMapping}s which this instance was configured with, it resolves the {@link ErrorType}
   * associated with the exception.
   *
   * Resolution is done based on the different mappings taking into account most specific error types first and then the more
   * general ones.
   *
   * If no error type can be find then {@link Optional#empty()} will be returned.
   *
   * @param exceptionType the exception used to resolve the error type.
   * @return optional created with the found error type, if any, or an empty optional.
   */
  public Optional<ErrorType> resolveErrorType(Class<? extends Throwable> exceptionType) {
    return exceptionMappings.stream()
        .filter(exceptionMapping -> exceptionMapping.matches(exceptionType))
        .findFirst()
        .map(ExceptionMapping::getErrorType);
  }

  /**
   * @return a builder to create instances of {@link ExceptionMapper}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to create instances of {@link ExceptionMapper}
   * 
   * @since 4.0
   */
  public static class Builder {

    private Builder() {}

    private Set<ExceptionMapping> exceptionMappings = new TreeSet<>();

    /**
     * Adds a mapping between an exception and an error type.
     * 
     * @param exceptionType the exception type.
     * @param errorType the error type.
     * @return {@code this} builder
     */
    public Builder addExceptionMapping(Class<? extends Throwable> exceptionType, ErrorType errorType) {
      if (!exceptionMappings.add(new ExceptionMapping(exceptionType, errorType))) {
        throw new MuleRuntimeException(createStaticMessage(format("Cannot build an %s with a repeated mapping for exception %s",
                                                                  ExceptionMapper.class.getName(),
                                                                  exceptionType.getClass().getName())));
      }
      return this;
    }

    /**
     * @return new instance of {@link ExceptionMapper} based on the provided configuration.
     */
    public ExceptionMapper build() {
      return new ExceptionMapper(exceptionMappings);
    }
  }
}
