/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.api.streaming.iterator.ListConsumer;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.ResultTransformer;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.sdk.api.runtime.streaming.PagingProvider;

/**
 * {@link ResultTransformer} implementation that transforms {@link PagingProvider} instances into
 * {@link ConsumerStreamingIterator} ones
 *
 * @since 4.5.0
 */
public class PagingResultTransformer implements ResultTransformer {

  private final ExtensionConnectionSupplier connectionSupplier;
  private final boolean supportsOAuth;

  private ComponentTracer<CoreEvent> operationConnectionTracer = DummyComponentTracerFactory.DUMMY_COMPONENT_TRACER_INSTANCE;

  public PagingResultTransformer(ExtensionConnectionSupplier connectionSupplier,
                                 boolean supportsOAuth) {
    this.connectionSupplier = connectionSupplier;
    this.supportsOAuth = supportsOAuth;
  }

  public PagingResultTransformer(ExtensionConnectionSupplier connectionSupplier,
                                 boolean supportsOAuth, ComponentTracer<CoreEvent> operationConnectionTracer) {
    this(connectionSupplier, supportsOAuth);
    this.operationConnectionTracer = operationConnectionTracer;
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
                                                      operationConnectionTracer);

    ListConsumer<?> consumer = new ListConsumer(producer);
    consumer.loadNextPage();
    return new ConsumerStreamingIterator<>(consumer);
  }

  public void setOperationConnectionTracer(ComponentTracer<CoreEvent> coreEventComponentTracer) {
    this.operationConnectionTracer = coreEventComponentTracer;
  }
}
