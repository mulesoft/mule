/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.construct;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.construct.Operation.Builder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

/**
 * Default {@link Builder} implementation
 *
 * @since 4.5.0
 */
class DefaultOperationBuilder implements Builder {

  private OperationModel operationModel;
  private MuleContext muleContext;
  private List<Processor> processors = emptyList();

  private MuleOperation product;


  /**
   * {@inheritDoc}
   */
  @Override
  public Builder processors(List<Processor> processors) {
    checkImmutable();
    checkArgument(processors != null, "processors cannot be null");
    this.processors = processors;

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Builder processors(Processor... processors) {
    checkImmutable();
    this.processors = asList(processors);

    return this;
  }

  @Override
  public Builder setOperationModel(OperationModel operationModel) {
    this.operationModel = operationModel;
    return this;
  }

  @Override
  public Builder setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    return this;
  }

  /**
   * Builds a flow with the provided configuration.
   *
   * @return a new flow instance.
   */
  @Override
  public Operation build() {
    checkImmutable();
    checkInvoked(operationModel, "setOperationModel(OperationModel)");
    checkInvoked(muleContext, "setMuleContext(MuleContext)");
    checkState(processors != null && !processors.isEmpty(), "Processors cannot be null nor empty");

    DefaultMessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder();
    chainBuilder.chain(processors);

    product = new MuleOperation(
        chainBuilder.build(),
        operationModel,
        muleContext);

    return product;
  }

  private final void checkImmutable() {
    if (product != null) {
      throw new IllegalStateException("Cannot change attributes once the operation was built");
    }
  }

  private void checkInvoked(Object value, String methodName) {
    checkState(value != null, methodName + " not invoked");
  }
}
