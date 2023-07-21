/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
