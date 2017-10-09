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
import static org.mule.runtime.core.api.exception.Errors.Identifiers.COMPOSITE_ROUTING_ERROR;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.DUPLICATE_MESSAGE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.EXPRESSION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.FATAL_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.NOT_PERMITTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.OVERLOAD_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.RETRY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ROUTING_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SECURITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SERVER_SECURITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_ERROR_RESPONSE_GENERATE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_ERROR_RESPONSE_SEND_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_RESPONSE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_RESPONSE_GENERATE_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.SOURCE_RESPONSE_SEND_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.STREAM_MAXIMUM_SIZE_EXCEEDED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.TIMEOUT_ERROR_IDENTIFIER;
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
     * Wild card that matches with any error and is on top of the error hierarchy for those that allow handling
     */
    public static final String ANY_IDENTIFIER = "ANY";

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
     * Indicates that the max attempts to reprocess a message from a source have been exhausted
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
     * Indicates that the maximum size allowed for a stream has been exceeded.
     */
    public static final String STREAM_MAXIMUM_SIZE_EXCEEDED_ERROR_IDENTIFIER = "STREAM_MAXIMUM_SIZE_EXCEEDED";

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
     * Indicates a security restriction enforced by a filter
     */
    public static final String NOT_PERMITTED_ERROR_IDENTIFIER = "NOT_PERMITTED";

    /**
     * Indicates that a timeout occurred during processing.
     */
    public static final String TIMEOUT_ERROR_IDENTIFIER = "TIMEOUT";

    /**
     * Indicates one or more errors occurred during routing.
     */
    public static final String COMPOSITE_ROUTING_ERROR = "COMPOSITE_ROUTING";

    /**
     * Indicates that an error occurred in the source of a flow.
     */
    public static final String SOURCE_ERROR_IDENTIFIER = "SOURCE";

    /**
     * Indicates that an error occurred in the source of the flow processing a successful response. Can only be propagated since
     * sources have already executed successful path.
     */
    public static final String SOURCE_RESPONSE_ERROR_IDENTIFIER = "SOURCE_RESPONSE";

    /**
     * Indicates that an error occurred in the source of the flow sending a successful response.
     */
    public static final String SOURCE_RESPONSE_SEND_ERROR_IDENTIFIER = "SOURCE_RESPONSE_SEND";

    /**
     * Indicates that an error occurred in the source of the flow generating the parameters of a successful response.
     */
    public static final String SOURCE_RESPONSE_GENERATE_ERROR_IDENTIFIER = "SOURCE_RESPONSE_GENERATE";

    // UNHANDLEABLE BUT AVAILABLE

    /**
     * Indicates that an error occurred in the source of the flow sending an error response. Configured error handling will not
     * execute since sources have already executed failing path.
     */
    public static final String SOURCE_ERROR_RESPONSE_SEND_ERROR_IDENTIFIER = "SOURCE_ERROR_RESPONSE_SEND";

    /**
     * Indicates that an error occurred in the source of the flow generating the parameters of an error response. Configured error
     * handling will not execute since sources have already executed failing path.
     */
    public static final String SOURCE_ERROR_RESPONSE_GENERATE_ERROR_IDENTIFIER = "SOURCE_ERROR_RESPONSE_GENERATE";

    /**
     * Indicates that an unknown and unexpected error occurred. Cannot be handled directly, only through ANY.
     */
    public static final String UNKNOWN_ERROR_IDENTIFIER = "UNKNOWN";

    // UNHANDLEABLE

    /**
     * Indicates that a severe error occurred. Cannot be handled. Top of the error hierarchy for those that do not allow handling.
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

    public static final class Handleable {

      public static final ComponentIdentifier ANY =
          builder().namespace(CORE_NAMESPACE_NAME).name(ANY_IDENTIFIER).build();
      public static final ComponentIdentifier TRANSFORMATION =
          builder().namespace(CORE_NAMESPACE_NAME).name(TRANSFORMATION_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier EXPRESSION =
          builder().namespace(CORE_NAMESPACE_NAME).name(EXPRESSION_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier VALIDATION =
          builder().namespace(CORE_NAMESPACE_NAME).name(VALIDATION_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier DUPLICATE_MESSAGE =
          builder().namespace(CORE_NAMESPACE_NAME).name(DUPLICATE_MESSAGE_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier REDELIVERY_EXHAUSTED = builder()
          .namespace(CORE_NAMESPACE_NAME).name(REDELIVERY_EXHAUSTED_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier RETRY_EXHAUSTED = builder()
          .namespace(CORE_NAMESPACE_NAME).name(RETRY_EXHAUSTED_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier ROUTING =
          builder().namespace(CORE_NAMESPACE_NAME).name(ROUTING_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier CONNECTIVITY =
          builder().namespace(CORE_NAMESPACE_NAME).name(CONNECTIVITY_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier SECURITY =
          builder().namespace(CORE_NAMESPACE_NAME).name(SECURITY_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier CLIENT_SECURITY =
          builder().namespace(CORE_NAMESPACE_NAME).name(CLIENT_SECURITY_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier SERVER_SECURITY =
          builder().namespace(CORE_NAMESPACE_NAME).name(SERVER_SECURITY_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier NOT_PERMITTED =
          builder().namespace(CORE_NAMESPACE_NAME).name(NOT_PERMITTED_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier STREAM_MAXIMUM_SIZE_EXCEEDED =
          builder().namespace(CORE_NAMESPACE_NAME).name(STREAM_MAXIMUM_SIZE_EXCEEDED_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier TIMEOUT =
          builder().namespace(CORE_NAMESPACE_NAME).name(TIMEOUT_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier COMPOSITE_ROUTING =
          builder().namespace(CORE_NAMESPACE_NAME).name(COMPOSITE_ROUTING_ERROR).build();
      public static final ComponentIdentifier UNKNOWN =
          builder().namespace(CORE_NAMESPACE_NAME).name(UNKNOWN_ERROR_IDENTIFIER).build();

      public static final ComponentIdentifier SOURCE =
          builder().namespace(CORE_NAMESPACE_NAME).name(SOURCE_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier SOURCE_RESPONSE =
          builder().namespace(CORE_NAMESPACE_NAME).name(SOURCE_RESPONSE_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier SOURCE_RESPONSE_GENERATE =
          builder().namespace(CORE_NAMESPACE_NAME).name(SOURCE_RESPONSE_GENERATE_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier SOURCE_RESPONSE_SEND =
          builder().namespace(CORE_NAMESPACE_NAME).name(SOURCE_RESPONSE_SEND_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier SOURCE_ERROR_RESPONSE_GENERATE =
          builder().namespace(CORE_NAMESPACE_NAME).name(SOURCE_ERROR_RESPONSE_GENERATE_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier SOURCE_ERROR_RESPONSE_SEND =
          builder().namespace(CORE_NAMESPACE_NAME).name(SOURCE_ERROR_RESPONSE_SEND_ERROR_IDENTIFIER).build();

    }

    public static final class Unhandleable {

      public static final ComponentIdentifier CRITICAL =
          builder().namespace(CORE_NAMESPACE_NAME).name(CRITICAL_IDENTIFIER).build();
      public static final ComponentIdentifier OVERLOAD =
          builder().namespace(CORE_NAMESPACE_NAME).name(OVERLOAD_ERROR_IDENTIFIER).build();
      public static final ComponentIdentifier FATAL =
          builder().namespace(CORE_NAMESPACE_NAME).name(FATAL_ERROR_IDENTIFIER).build();

    }

  }
}
