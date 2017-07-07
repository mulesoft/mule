/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.streaming.object.iterator.Consumer;
import org.mule.runtime.core.internal.streaming.object.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.internal.streaming.object.iterator.ListConsumer;
import org.mule.runtime.core.internal.streaming.object.iterator.Producer;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingProviderProducer;

import reactor.core.publisher.Mono;

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
                                        ResolverSet resolverSet,
                                        CursorProviderFactory cursorProviderFactory,
                                        ExtensionManager extensionManager,
                                        PolicyManager policyManager,
                                        ExtensionConnectionSupplier connectionSupplier) {
    super(extensionModel, operationModel, configurationProvider, target, resolverSet, cursorProviderFactory,
          extensionManager, policyManager);
    this.connectionSupplier = connectionSupplier;
  }

  @Override
  protected Mono<Event> doProcess(Event event, ExecutionContextAdapter<OperationModel> operationContext) {
    try {
      Event resultEvent = super.doProcess(event, operationContext).block();
      PagingProvider<?, ?> pagingProvider = getTarget()
          .map(target -> getPagingProvider(
                                           (Message) resultEvent.getVariable(target).getValue()))
          .orElseGet(() -> getPagingProvider(resultEvent.getMessage()));

      if (pagingProvider == null) {
        throw new IllegalStateException("Obtained paging delegate cannot be null");
      }

      Producer<?> producer =
          new PagingProviderProducer(pagingProvider, operationContext.getConfiguration().get(),
                                     operationContext, connectionSupplier);
      Consumer<?> consumer = new ListConsumer(producer);

      return just(returnDelegate.asReturnValue(new ConsumerStreamingIterator<>(consumer), operationContext));
    } catch (Exception e) {
      return error(e);
    } catch (Throwable t) {
      return error(wrapFatal(t));
    }
  }

  private PagingProvider getPagingProvider(Message message) {
    return (PagingProvider) message.getPayload().getValue();
  }
}
