/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.exception.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * Enum that defines all the Mule Error Types that can be thrown by the modules that use the soap service.
 *
 * @since 4.0
 */
public enum SoapErrors implements ErrorTypeDefinition<SoapErrors> {

  /**
   * Error thrown when an invalid WSDL is found or inconsistent WSDL state occurred.
   */
  INVALID_WSDL,

  /**
   * Error thrown when an encoding related problem occurs when parsing the request or response XML.
   */
  ENCODING,

  /**
   * Error thrown when the generated request is invalid, mostly because inconsistent provided parameters.
   */
  BAD_REQUEST,

  /**
   * Error thrown when the resulting response is invalid.
   */
  BAD_RESPONSE,

  /**
   *  a problem occurred while sending the request.
   */
  CANNOT_DISPATCH,

  /**
   * Error thrown when the outgoing request took longer than the server prepared to wait.
   */
  TIMEOUT,

  /**
   * Error thrown when a SOAP Fault occurred.
   */
  SOAP_FAULT
}
