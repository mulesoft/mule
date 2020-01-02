/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.supportsOAuth;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.api.streaming.iterator.ListConsumer;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingProviderProducer;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

/**
 * Provides instances of {@link OperationMessageProcessor} for a given {@link OperationModel}
 *
 * @since 4.0
 */
public final class OperationMessageProcessorBuilder
    extends ComponentMessageProcessorBuilder<OperationModel, OperationMessageProcessor> {

  public OperationMessageProcessorBuilder(ExtensionModel extension,
                                          OperationModel operation,
                                          PolicyManager policyManager,
                                          MuleContext muleContext,
                                          Registry registry) {

    super(extension, operation, policyManager, registry.lookupByType(ReflectionCache.class).get(),
          registry.lookupByType(ExpressionManager.class).get(), muleContext, registry);

  }

  @Override
  protected OperationMessageProcessor createMessageProcessor(ExtensionManager extensionManager, ResolverSet arguments) {
    ConfigurationProvider configurationProvider = getConfigurationProvider();
    OperationMessageProcessor processor;
    DefaultExecutionMediator.ValueTransformer valueTransformer = null;
    if (operationModel.getModelProperty(PagedOperationModelProperty.class).isPresent()) {
      valueTransformer = transformPagingDelegate(extensionConnectionSupplier);
    }
    if (supportsOAuth(extensionModel)) {
      processor = new OAuthOperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, targetValue,
                                                     arguments, cursorProviderFactory, retryPolicyTemplate, extensionManager,
                                                     policyManager, reflectionCache, valueTransformer);
    } else {
      processor = new OperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, targetValue,
                                                arguments, cursorProviderFactory, retryPolicyTemplate, extensionManager,
                                                policyManager,
                                                reflectionCache, valueTransformer);
    }
    return processor;
  }

  private DefaultExecutionMediator.ValueTransformer transformPagingDelegate(ExtensionConnectionSupplier connectionSupplier) {
    return (operationContext, value) -> {
      if (value == null) {
        throw new IllegalStateException("Obtained paging delegate cannot be null");
      }
      ConfigurationInstance config = (ConfigurationInstance) operationContext.getConfiguration().get();
      Producer<?> producer = new PagingProviderProducer((PagingProvider) value, config, operationContext, connectionSupplier);
      ListConsumer<?> consumer = new ListConsumer(producer);
      consumer.loadNextPage();
      return new ConsumerStreamingIterator<>(consumer);
    };
  }
}
