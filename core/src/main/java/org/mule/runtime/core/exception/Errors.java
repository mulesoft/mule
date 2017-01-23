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
import static org.mule.runtime.core.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.CORE_NAMESPACE;

import org.mule.runtime.dsl.api.component.config.ComponentIdentifier;

/**
 * Provides the constants for the core error types
 */
public abstract class Errors {

  public static final String CORE_NAMESPACE_NAME = CORE_NAMESPACE.toUpperCase();

  public static final class Identifiers {

    // HANDLEABLE

    /**
     * Indicates that a problem occurred when transforming a value
     */
    public static final String TRANSFORMATION_ERROR_IDENTIFIER = "TRANSFORMATION";

    /**
     * Indicates that a problem occurred when resolving an expression
     */
    public static final String EXPRESSION_ERROR_IDENTIFIER = "EXPRESSION";

    /**
     * Indicates that the retry policy, of a certain component, to execute some action, eg: connectivity, delivery has
     * been exhausted
     */
    public static final String REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER = "REDELIVERY_EXHAUSTED";

    /**
     * Indicates that the retry of a certain execution block has been exhausted
     */
    public static final String RETRY_EXHAUSTED_ERROR_IDENTIFIER = "RETRY_EXHAUSTED";

    /**
     * Indicates that a problem occurred when routing a message
     */
    public static final String ROUTING_ERROR_IDENTIFIER = "ROUTING";

    /**
     * Indicates that a problem occurred and a connection could not be established
     */
    public static final String CONNECTIVITY_ERROR_IDENTIFIER = "CONNECTIVITY";

    /**
     * Indicates a security type problem occurred, eg: invalid credentials, expired token, etc.
     */
    public static final String SECURITY_ERROR_IDENTIFIER = "SECURITY";

    /**
     * Wild card that matches with any error
     */
    public static final String ANY_IDENTIFIER = "ANY";

    // UNHANDLEABLE

    /**
     * Indicates that an unknown and unexpected error occurred. Cannot be handled directly, only through ANY.
     */
    public static final String UNKNOWN_ERROR_IDENTIFIER = "UNKNOWN";

    /**
     * Indicates that a severe error occurred. Cannot be handled. Other unhandleable errors should go under it.
     */
    public static final String CRITICAL_IDENTIFIER = "CRITICAL";

    /**
     * Indicates a problem of overloading occurred and the execution was rejected. Cannot be handled.
     */
    public static final String OVERLOAD_ERROR_IDENTIFIER = "OVERLOAD";

  }

  public static final class ComponentIdentifiers {

    public static final ComponentIdentifier ANY =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE).withName(ANY_IDENTIFIER).build();
    public static final ComponentIdentifier CRITICAL =
        new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE).withName(CRITICAL_IDENTIFIER).build();
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
