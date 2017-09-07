/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.api.streaming.iterator.ListConsumer;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingProviderProducer;

import java.util.Optional;

/**
 * A specialization of {@link OperationMessageProcessor} which supports auto paging by the means of a
 * {@link ConsumerStreamingIterator}
 *
 * @since 4.0
 */
public class PagedOperationMessageProcessor extends OperationMessageProcessor {

  private final ExtensionConnectionSupplier connectionSupplier;

  public PagedOperationMessageProcessor(ExtensionModel extensionModel,
                                        OperationModel operationModel,
                                        ConfigurationProvider configurationProvider,
                                        String target,
                                        String targetValue,
                                        ResolverSet resolverSet,
                                        CursorProviderFactory cursorProviderFactory,
                                        RetryPolicyTemplate retryPolicyTemplate,
                                        ExtensionManager extensionManager,
                                        PolicyManager policyManager,
                                        ExtensionConnectionSupplier connectionSupplier) {
    super(extensionModel, operationModel, configurationProvider, target, targetValue, resolverSet, cursorProviderFactory,
          retryPolicyTemplate, extensionManager, policyManager);
    this.connectionSupplier = connectionSupplier;
  }

  private DefaultExecutionMediator.ValueTransformer transformPagingDelegate() {
    return (operationContext, value) -> {
      if (value == null) {
        throw new IllegalStateException("Obtained paging delegate cannot be null");
      }
      Optional<ConfigurationInstance> config = operationContext.getConfiguration();
      Producer<?> producer =
          new PagingProviderProducer((PagingProvider) value, config.get(),
                                     operationContext, connectionSupplier);

      ListConsumer<?> consumer = new ListConsumer(producer);
      consumer.loadNextPage();
      return new ConsumerStreamingIterator<>(consumer);
    };
  }

  @Override
  protected ExecutionMediator createExecutionMediator() {
    return new DefaultExecutionMediator(extensionModel,
                                        componentModel,
                                        connectionManager,
                                        muleContext.getErrorTypeRepository(),
                                        transformPagingDelegate());
  }
}
