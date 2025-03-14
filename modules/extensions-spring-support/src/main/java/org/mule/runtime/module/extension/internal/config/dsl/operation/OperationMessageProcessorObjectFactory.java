/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static java.util.Collections.emptyList;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.ComponentMessageProcessorObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorBuilder;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.List;

import jakarta.inject.Inject;

/**
 * An {@link AbstractExtensionObjectFactory} which produces {@link OperationMessageProcessor} instances
 *
 * @since 4.0
 */
public class OperationMessageProcessorObjectFactory
    extends ComponentMessageProcessorObjectFactory<OperationModel, OperationMessageProcessor> {

  private List<EnrichedErrorMapping> errorMappings = emptyList();

  @Inject
  private ReflectionCache reflectionCache;
  @Inject
  private ExpressionManager expressionManager;
  @Inject
  private ExtensionConnectionSupplier extensionConnectionSupplier;
  @Inject
  private ComponentTracerFactory<CoreEvent> componentTracerFactory;

  public OperationMessageProcessorObjectFactory(ExtensionModel extensionModel,
                                                OperationModel componentModel,
                                                MuleContext muleContext) {
    super(extensionModel, componentModel, muleContext);
  }

  @Override
  protected OperationMessageProcessorBuilder getMessageProcessorBuilder() {
    return new OperationMessageProcessorBuilder(extensionModel, componentModel, errorMappings,
                                                reflectionCache,
                                                expressionManager,
                                                extensionConnectionSupplier,
                                                componentTracerFactory,
                                                muleContext);
  }

  public void setErrorMappings(List<EnrichedErrorMapping> errorMappings) {
    this.errorMappings = errorMappings;
  }
}
