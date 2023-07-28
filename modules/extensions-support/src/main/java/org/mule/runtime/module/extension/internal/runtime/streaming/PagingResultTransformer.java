/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * {@link ResultTransformer} implementation that transforms {@link PagingProvider} instances into
 * {@link ConsumerStreamingIterator} ones
 *
 * @since 4.5.0
 */
public class PagingResultTransformer implements ResultTransformer {

  private final ExtensionConnectionSupplier connectionSupplier;
  private final boolean supportsOAuth;
  private final InitialSpanInfo getConnectionSpanInfo;

  public PagingResultTransformer(ExtensionConnectionSupplier connectionSupplier, InitialSpanInfo getConnectionSpanInfo,
                                 boolean supportsOAuth) {
    this.connectionSupplier = connectionSupplier;
    this.supportsOAuth = supportsOAuth;
    this.getConnectionSpanInfo = getConnectionSpanInfo;
  }

  @Override
  public Object applyChecked(ExecutionContextAdapter operationContext, Object value) {
    if (value == null) {
      throw new IllegalStateException("Obtained paging delegate cannot be null");
    }
    ConfigurationInstance config = (ConfigurationInstance) operationContext.getConfiguration().get();
    Producer<?> producer = new PagingProviderProducer((PagingProvider) value,
                                                      config,
                                                      operationContext,
                                                      connectionSupplier,
                                                      supportsOAuth,
                                                      getConnectionSpanInfo);

    ListConsumer<?> consumer = new ListConsumer(producer);
    consumer.loadNextPage();
    return new ConsumerStreamingIterator<>(consumer);
  }
}
