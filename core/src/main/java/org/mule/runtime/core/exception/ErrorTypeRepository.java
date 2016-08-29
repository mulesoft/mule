/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.lang.String.format;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.message.ErrorTypeBuilder;

/**
 * Repository for the different {@link ErrorType}s in a mule artifact.
 *
 * Only once instance of {@link ErrorType} must exists describing the same
 * combination of error identifier and namespace.
 *
 * @since 4.0
 */
public class ErrorTypeRepository {

  public static final String TRANSFORMATION_ERROR_IDENTIFIER = "TRANSFORMATION";
  public static final String EXPRESSION_ERROR_IDENTIFIER = "EXPRESSION";
  public static final String UNKNOWN_ERROR_IDENTIFIER = "UNKNOWN";
  public static final String ANY_IDENTIFIER = "ANY";
  public static final String CORE_NAMESPACE_NAME = "mule";

  public static final ErrorType ANY_ERROR_TYPE =
      ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).stringRepresentation(ANY_IDENTIFIER).build();
  private Map<ErrorTypeKey, ErrorType> errorTypes = new HashMap<>();

  public ErrorTypeRepository() {
    this.errorTypes.put(new ErrorTypeKey(CORE_NAMESPACE_NAME, ANY_IDENTIFIER),
                        ANY_ERROR_TYPE);

  }

  public void addErrorType(String namespace, String stringRepresentation, ErrorType parentErrorType) {
    checkArgument(namespace != null, "namespace cannot be null");
    checkArgument(stringRepresentation != null, "string representation cannot be null");
    ErrorTypeBuilder errorTypeBuilder =
        ErrorTypeBuilder.builder().namespace(namespace).stringRepresentation(stringRepresentation)
            .parentErrorType(parentErrorType);
    if (this.errorTypes.put(new ErrorTypeKey(namespace, stringRepresentation), errorTypeBuilder.build()) != null) {
      throw new IllegalStateException(format("Already exists an error type with namespace %s and string representation",
                                             namespace, stringRepresentation));
    }
  }

  public ErrorType lookupErrorType(String namespace, String stringRepresentation) {
    ErrorType errorType = this.errorTypes.get(new ErrorTypeKey(namespace, stringRepresentation));
    if (errorType == null) {
      throw new IllegalStateException(format("there's no error type with namespace %s and string representation", namespace,
                                             stringRepresentation));
    }
    return errorType;
  }

  public ErrorType getAnyErrorType() {
    return ANY_ERROR_TYPE;
  }

  private static final class ErrorTypeKey {

    private String namespace;
    private String errorTypeRepresentation;

    public ErrorTypeKey(String namespace, String errorTypeRepresentation) {
      this.namespace = namespace;
      this.errorTypeRepresentation = errorTypeRepresentation;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ErrorTypeKey that =
          (ErrorTypeKey) o;

      if (!namespace.equals(that.namespace)) {
        return false;
      }
      return errorTypeRepresentation.equals(that.errorTypeRepresentation);

    }

    @Override
    public int hashCode() {
      int result = namespace.hashCode();
      result = 31 * result + errorTypeRepresentation.hashCode();
      return result;
    }
  }



}
