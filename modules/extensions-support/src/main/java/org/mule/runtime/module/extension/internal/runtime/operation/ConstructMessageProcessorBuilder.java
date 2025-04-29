/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

/**
 * Provides instances of {@link ConstructMessageProcessor} for a given {@link ConstructModel}
 *
 * @since 4.0
 */
public final class ConstructMessageProcessorBuilder
    extends ComponentMessageProcessorBuilder<ConstructModel, ConstructMessageProcessor> {

  public ConstructMessageProcessorBuilder(ExtensionModel extensionModel,
                                          ConstructModel operationModel,
                                          ReflectionCache reflectionCache,
                                          ExtendedExpressionManager expressionManager,
                                          ExtensionConnectionSupplier extensionConnectionSupplier,
                                          ComponentTracerFactory<CoreEvent> componentTracerFactory,
                                          MuleContext muleContext) {
    super(extensionModel, operationModel,
          reflectionCache, expressionManager, extensionConnectionSupplier,
          muleContext);
  }

  @Override
  protected ConstructMessageProcessor createMessageProcessor(ExtensionManager extensionManager, ResolverSet arguments) {
    return new ConstructMessageProcessor(extensionModel, operationModel,
                                         getConfigurationProviderResolver(), target, targetValue,
                                         arguments,
                                         cursorProviderFactory, retryPolicyTemplate, nestedChain, classLoader,
                                         extensionManager,
                                         reflectionCache,
                                         terminationTimeout);
  }

}
