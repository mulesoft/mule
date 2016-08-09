/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.client;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.client.AbstractConnectorMessageProcessorProvider;
import org.mule.runtime.core.api.client.OperationOptions;
import org.mule.runtime.core.api.client.RequestCacheKey;
import org.mule.runtime.core.api.processor.MessageProcessor;

/**
 * Provider for operations of the Test Connector.
 */
public class TestConnectorMessageProcessorProvider extends AbstractConnectorMessageProcessorProvider {

  public static final String TEST_URL_PREFIX = "test://";

  @Override
  protected MessageProcessor buildMessageProcessor(RequestCacheKey cacheKey) throws MuleException {
    final String queueName = cacheKey.getUrl().substring(TEST_URL_PREFIX.length());
    final OperationOptions operationOptions = cacheKey.getOperationOptions();

    return new QueueReaderMessageProcessor(muleContext, queueName, operationOptions.getResponseTimeout());
  }

  @Override
  public boolean supportsUrl(String url) {
    return url.startsWith(TEST_URL_PREFIX);
  }
}
