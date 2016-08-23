/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.streaming.Consumer;
import org.mule.runtime.core.streaming.ConsumerIterator;
import org.mule.runtime.core.streaming.ListConsumer;
import org.mule.runtime.core.streaming.Producer;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.streaming.PagingProvider;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingProviderProducer;

/**
 * A specialization of {@link OperationMessageProcessor} which also implements {@link InterceptingMessageProcessor}.
 *
 * @since 4.0
 */
public class PagedOperationMessageProcessor extends OperationMessageProcessor {

  public PagedOperationMessageProcessor(RuntimeExtensionModel extensionModel,
                                        RuntimeOperationModel operationModel,
                                        String configurationProviderName,
                                        String target,
                                        ResolverSet resolverSet,
                                        ExtensionManagerAdapter extensionManager) {
    super(extensionModel, operationModel, configurationProviderName, target, resolverSet, extensionManager);
  }

  @Override
  protected org.mule.runtime.api.message.MuleEvent doProcess(MuleEvent event, OperationContextAdapter operationContext)
      throws MuleException {
    MuleEvent resultEvent = (MuleEvent) super.doProcess(event, operationContext);
    PagingProvider pagingProvider = resultEvent.getMessage().getPayload();

    if (pagingProvider == null) {
      throw new IllegalStateException("Obtained paging delegate cannot be null");
    }

    Producer<?> producer = new PagingProviderProducer(pagingProvider, operationContext.getConfiguration(), connectionManager);
    Consumer<?> consumer = new ListConsumer(producer);

    return returnDelegate.asReturnValue(new ConsumerIterator<>(consumer), operationContext);
  }

}
