/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.EXPRESSION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.OVERLOAD_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.RETRY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.ROUTING_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.SECURITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.TRANSFORMATION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import static org.mule.runtime.dsl.api.xml.DslConstants.CORE_NAMESPACE;

import org.mule.runtime.dsl.api.component.ComponentIdentifier;

/**
 * Provides the constants for the core error types
 */
public abstract class Errors {

  public static final String CORE_NAMESPACE_NAME = CORE_NAMESPACE.toUpperCase();

  public static final class Identifiers {

    /**
     * Indicates that a problem occurred when transforming a value
     */
    public static final String TRANSFORMATION_ERROR_IDENTIFIER = "TRANSFORMATION";

    /**
     * Indicates that a problem occurred when resolving an expression
     */
    public static final String EXPRESSION_ERROR_IDENTIFIER = "EXPRESSION";

    /**
     * Indicate that the retry policy, of a certain component, to execute some action, eg: connectivity, delivery has
     * been exhausted
     */
    public static final String REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER = "REDELIVERY_EXHAUSTED";

    /**
     * Indicates that the retrial of a certain execution block has been exhausted
     */
    public static final String RETRY_EXHAUSTED_ERROR_IDENTIFIER = "RETRY_EXHAUSTED";

    /**
     * Indicates that a problem occurred when routing a message
     */
    public static final String ROUTING_ERROR_IDENTIFIER = "ROUTING";

    /**
     * Indicates that was a problem and could not establish a connection
     */
    public static final String CONNECTIVITY_ERROR_IDENTIFIER = "CONNECTIVITY";

    /**
     * Indicates a problem of security type, eg: invalid credentials, expired token, etc.
     */
    public static final String SECURITY_ERROR_IDENTIFIER = "SECURITY";

    /**
     * Indicates a problem of overloading and the execution was rejected
     */
    public static final String OVERLOAD_ERROR_IDENTIFIER = "OVERLOAD";

    /**
     * Indicates that an unknown and unexpected error occurred
     */
    public static final String UNKNOWN_ERROR_IDENTIFIER = "UNKNOWN";

    /**
     * Indicates that a severe and un handleable error occurred
     */
    public static final String CRITICAL_IDENTIFIER = "CRITICAL";

    /**
     * Wild card that matches with any error
     */
    public static final String ANY_IDENTIFIER = "ANY";

  }

  public static final class ComponentIdentifiers {

    public static final ComponentIdentifier TRANSFORMATION =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(TRANSFORMATION_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier EXPRESSION =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(EXPRESSION_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier REDELIVERY_EXHAUSTED = new ComponentIdentifier.Builder()
        .withNamespace(CORE_NAMESPACE_NAME).withName(REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier RETRY_EXHAUSTED = new ComponentIdentifier.Builder()
        .withNamespace(CORE_NAMESPACE_NAME).withName(RETRY_EXHAUSTED_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier ROUTING =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(ROUTING_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier CONNECTIVITY =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(CONNECTIVITY_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier SECURITY =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(SECURITY_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier OVERLOAD =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(OVERLOAD_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier UNKNOWN =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(UNKNOWN_ERROR_IDENTIFIER).build();
  }
}
