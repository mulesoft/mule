/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.streaming.Consumer;
import org.mule.runtime.core.streaming.ConsumerIterator;
import org.mule.runtime.core.streaming.ListConsumer;
import org.mule.runtime.core.streaming.Producer;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingProviderProducer;

/**
 * A specialization of {@link OperationMessageProcessor} which also implements {@link InterceptingMessageProcessor}.
 *
 * @since 4.0
 */
public class PagedOperationMessageProcessor extends OperationMessageProcessor {

  public PagedOperationMessageProcessor(ExtensionModel extensionModel,
                                        OperationModel operationModel,
                                        ConfigurationProvider configurationProvider,
                                        String target,
                                        ResolverSet resolverSet,
                                        ExtensionManagerAdapter extensionManager) {
    super(extensionModel, operationModel, configurationProvider, target, resolverSet, extensionManager);
  }

  @Override
  protected org.mule.runtime.api.message.MuleEvent doProcess(Event event, ExecutionContextAdapter operationContext)
      throws MuleException {

    Event resultEvent = (Event) super.doProcess(event, operationContext);
    PagingProvider<?, ?> pagingProvider = (PagingProvider) resultEvent.getMessage().getPayload().getValue();

    if (pagingProvider == null) {
      throw new IllegalStateException("Obtained paging delegate cannot be null");
    }

    Producer<?> producer =
        new PagingProviderProducer(pagingProvider, (ConfigurationInstance) operationContext.getConfiguration().get(),
                                   connectionManager);
    Consumer<?> consumer = new ListConsumer(producer);

    return returnDelegate.asReturnValue(new ConsumerIterator<>(consumer), operationContext);
  }

}
