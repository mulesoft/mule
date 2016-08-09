/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request.client;

import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.client.AbstractConnectorMessageProcessorProvider;
import org.mule.runtime.core.api.client.OperationOptions;
import org.mule.runtime.core.api.client.RequestCacheKey;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;

/**
 * Provider for operations of the HTTP module.
 */
public class HttpConnectorMessageProcessorProvider extends AbstractConnectorMessageProcessorProvider {

  @Override
  public boolean supportsUrl(String url) {
    return url.startsWith(HTTP.getScheme()) || url.startsWith(HTTPS.getScheme());
  }

  @Override
  protected MessageProcessor buildMessageProcessor(final RequestCacheKey cacheKey) throws MuleException {
    final OperationOptions operationOptions = cacheKey.getOperationOptions();
    final MessageExchangePattern exchangePattern = cacheKey.getExchangePattern();
    final String url = cacheKey.getUrl();
    final HttpRequesterBuilder httpRequesterBuilder = new HttpRequesterBuilder(muleContext).setUrl(url);
    if (operationOptions instanceof HttpRequestOptions) {
      httpRequesterBuilder.setOperationConfig((HttpRequestOptions) operationOptions);
    } else {
      if (operationOptions.getResponseTimeout() != null) {
        httpRequesterBuilder.responseTimeout(operationOptions.getResponseTimeout());
      }
    }
    MessageProcessor messageProcessor = httpRequesterBuilder.build();
    if (exchangePattern.equals(MessageExchangePattern.ONE_WAY)) {
      messageProcessor = new OneWayHttpRequesterAdapter(messageProcessor);
    }
    return messageProcessor;
  }
}
