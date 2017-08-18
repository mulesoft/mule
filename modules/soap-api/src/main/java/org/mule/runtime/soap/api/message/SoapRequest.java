/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

/**
 * Represents a request that aims to be sent to a Soap Web Service.
 *
 * @since 4.0
 */
public interface SoapRequest extends SoapMessage {

  /**
   * @return The name of the operation that is requested.
   */
  String getOperation();

  /**
   * @return a {@link SoapRequestBuilder} instance to create a new {@link SoapRequest}.
   */
  static SoapRequestBuilder builder() {
    return new SoapRequestBuilder();
  }

  /**
   * @param operation the operation that is going to be executed.
   * @return an empty Soap Request with no content for the requested operation.
   */
  static SoapRequest empty(String operation) {
    return builder().operation(operation).build();
  }
}
