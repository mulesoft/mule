/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.connector;

import org.mule.runtime.core.api.MessageExchangePattern;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.client.OperationOptions;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Provider of operation for a Mule connector.
 *
 * A Mule connector can provide an implementation of this interface in the registry and mule will use it to create operations
 * using an URL and later executed them.
 *
 * The implementation must be located in the mule registry before the start phase.
 */
public interface ConnectorOperationProvider {

  /**
   * @param url an URL for creating an operation
   * @return true if the provider can handle the URL, false otherwise
   */
  boolean supportsUrl(String url);

  /**
   * A {@link org.mule.runtime.core.api.processor.Processor} that contains the behaviour for the URL
   *
   * @param url an URL for creating an operation
   * @param operationOptions the operation options
   * @param exchangePattern exchange pattern to use to execute the request.
   * @return a {@link org.mule.runtime.core.api.processor.Processor} that fulfills the operation
   * @throws MuleException
   */
  Processor getMessageProcessor(String url, OperationOptions operationOptions, MessageExchangePattern exchangePattern)
      throws MuleException;

}
