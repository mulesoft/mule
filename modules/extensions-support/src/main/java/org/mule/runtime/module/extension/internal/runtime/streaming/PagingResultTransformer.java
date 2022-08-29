/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.api.streaming.iterator.ListConsumer;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.ResultTransformer;

public class PagingResultTransformer implements ResultTransformer {

  private final ExtensionConnectionSupplier connectionSupplier;
  private final boolean supportsOAuth;

  public PagingResultTransformer(ExtensionConnectionSupplier connectionSupplier, boolean supportsOAuth) {
    this.connectionSupplier = connectionSupplier;
    this.supportsOAuth = supportsOAuth;
  }

  @Override
  public Object applyChecked(ExecutionContextAdapter operationContext, Object value) throws Throwable {
    if (value == null) {
      throw new IllegalStateException("Obtained paging delegate cannot be null");
    }
    ConfigurationInstance config = (ConfigurationInstance) operationContext.getConfiguration().get();
    Producer<?> producer = new PagingProviderProducer((PagingProvider) value,
                                                      config,
                                                      operationContext,
                                                      connectionSupplier,
                                                      supportsOAuth);

    ListConsumer<?> consumer = new ListConsumer(producer);
    consumer.loadNextPage();
    return new ConsumerStreamingIterator<>(consumer);
  }
}
