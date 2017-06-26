/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CLIENT_SECURITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.DUPLICATE_MESSAGE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.EXPRESSION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.FATAL_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.OVERLOAD_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.RETRY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ROUTING_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SECURITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SERVER_SECURITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_RESPONSE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.STREAM_MAXIMUM_SIZE_EXCEEDED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.TRANSFORMATION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.VALIDATION_ERROR_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import org.mule.runtime.api.component.ComponentIdentifier;

/**
 * Provides the constants for the core error types
 */
public abstract class Errors {

  public static final String CORE_NAMESPACE_NAME = CORE_PREFIX.toUpperCase();

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
     * Indicates that the execution of the current event is stopped
     */
    public static final String VALIDATION_ERROR_IDENTIFIER = "VALIDATION";

    /**
     * Indicates that the execution of the current event is stopped
     */
    public static final String DUPLICATE_MESSAGE_ERROR_IDENTIFIER = "DUPLICATE_MESSAGE";

    /**
     * Indicates that the retry policy, of a certain component, to execute some action, eg: connectivity, delivery has been
     * exhausted
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
     * Indicates a security type problem enforced by an external entity
     */
    public static final String CLIENT_SECURITY_ERROR_IDENTIFIER = "CLIENT_SECURITY";

    /**
     * Indicates a security type problem enforced by the mule runtime
     */
    public static final String SERVER_SECURITY_ERROR_IDENTIFIER = "SERVER_SECURITY";

    /**
     * Wild card that matches with any error
     */
    public static final String ANY_IDENTIFIER = "ANY";

    public static final String STREAM_MAXIMUM_SIZE_EXCEEDED_ERROR_IDENTIFIER = "STREAM_MAXIMUM_SIZE_EXCEEDED";

    // UNHANDLEABLE

    /**
     * Indicates that an unknown and unexpected error occurred. Cannot be handled directly, only through ANY.
     */
    public static final String UNKNOWN_ERROR_IDENTIFIER = "UNKNOWN";

    /**
     * Indicates that an error occurred in the source of the flow.
     */
    public static final String SOURCE_ERROR_IDENTIFIER = "SOURCE";

    /**
     * Indicates that an error occurred in the source of the flow processing a successful response.
     */
    public static final String SOURCE_RESPONSE_ERROR_IDENTIFIER = "SOURCE_RESPONSE";

    /**
     * Indicates that a severe error occurred. Cannot be handled. Other unhandleable errors should go under it.
     */
    public static final String CRITICAL_IDENTIFIER = "CRITICAL";

    /**
     * Indicates a problem of overloading occurred and the execution was rejected. Cannot be handled.
     */
    public static final String OVERLOAD_ERROR_IDENTIFIER = "OVERLOAD";

    /**
     * Indicates that a fatal error occurred (such as stack overflow). Cannot be handled.
     */
    public static final String FATAL_ERROR_IDENTIFIER = "FATAL_JVM_ERROR";
  }

  public static final class ComponentIdentifiers {

    public static final ComponentIdentifier ANY =
        builder().withNamespace(CORE_PREFIX).withName(ANY_IDENTIFIER).build();
    public static final ComponentIdentifier CRITICAL =
        builder().withNamespace(CORE_PREFIX).withName(CRITICAL_IDENTIFIER).build();
    public static final ComponentIdentifier TRANSFORMATION =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(TRANSFORMATION_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier EXPRESSION =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(EXPRESSION_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier VALIDATION =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(VALIDATION_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier DUPLICATE_MESSAGE =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(DUPLICATE_MESSAGE_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier REDELIVERY_EXHAUSTED = builder()
        .withNamespace(CORE_NAMESPACE_NAME).withName(REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier RETRY_EXHAUSTED = builder()
        .withNamespace(CORE_NAMESPACE_NAME).withName(RETRY_EXHAUSTED_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier ROUTING =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(ROUTING_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier CONNECTIVITY =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(CONNECTIVITY_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier SECURITY =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(SECURITY_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier CLIENT_SECURITY =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(CLIENT_SECURITY_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier SERVER_SECURITY =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(SERVER_SECURITY_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier OVERLOAD =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(OVERLOAD_ERROR_IDENTIFIER).build();

    public static final ComponentIdentifier SOURCE =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(SOURCE_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier SOURCE_RESPONSE =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(SOURCE_RESPONSE_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier SOURCE_RESPONSE_GENERATE =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName("SOURCE_RESPONSE_GENERATE").build();
    public static final ComponentIdentifier SOURCE_RESPONSE_SEND =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName("SOURCE_RESPONSE_SEND").build();
    public static final ComponentIdentifier SOURCE_ERROR_RESPONSE_GENERATE =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName("SOURCE_ERROR_RESPONSE_GENERATE").build();
    public static final ComponentIdentifier SOURCE_ERROR_RESPONSE_SEND =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName("SOURCE_ERROR_RESPONSE_SEND").build();

    public static final ComponentIdentifier STREAM_MAXIMUM_SIZE_EXCEEDED =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(STREAM_MAXIMUM_SIZE_EXCEEDED_ERROR_IDENTIFIER).build();
    public static final ComponentIdentifier FATAL =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(FATAL_ERROR_IDENTIFIER).build();

    public static final ComponentIdentifier UNKNOWN =
        builder().withNamespace(CORE_NAMESPACE_NAME).withName(UNKNOWN_ERROR_IDENTIFIER).build();

  }
}
