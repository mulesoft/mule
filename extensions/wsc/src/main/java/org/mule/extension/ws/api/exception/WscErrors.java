/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.exception;

import static java.util.Optional.ofNullable;
import org.mule.extension.ws.internal.WebServiceConsumer;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Optional;

/**
 * Enum that defines all the Mule Error Types that can be thrown by the {@link WebServiceConsumer}.
 *
 * @since 4.0
 */
public enum WscErrors implements ErrorTypeDefinition<WscErrors> {

  /**
   * Error thrown when an invalid WSDL is found or inconsistent WSDL state occurred.
   */
  INVALID_WSDL("Invalid WSDL"),

  /**
   * Error thrown when an encoding related problem occurs when parsing the request or response XML.
   */
  ENCODING("XML Encoding"),

  /**
   * Error thrown when the generated request is invalid, mostly because inconsistent provided parameters.
   */
  BAD_REQUEST("Bad Request"),

  /**
   * Error thrown when the resulting response is invalid.
   */
  BAD_RESPONSE("Bad Response"),

  /**
   * Error thrown when a SOAP Fault occurred.
   */
  SOAP_FAULT("Soap Fault");

  private final String type;
  private final ErrorTypeDefinition parent;

  WscErrors(String type, ErrorTypeDefinition parent) {
    this.type = type;
    this.parent = parent;
  }

  WscErrors(String type) {
    this.type = type;
    this.parent = null;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parent);
  }
}
