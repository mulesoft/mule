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

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.construct.Operation.Builder;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

/**
 * Default {@link Builder} implementation
 *
 * @since 4.5.0
 */
public class DefaultOperationBuilder implements Builder {

  private final OperationModel model;
  private final MuleContext muleContext;
  private List<Processor> processors = emptyList();

  private DefaultOperation operation;

  /**
   * Creates a new builder
   *
   * @param model        the model for the operation
   * @param muleContext context where the operation will be associated with.
   */
  public DefaultOperationBuilder(OperationModel model, MuleContext muleContext) {
    checkArgument(model != null, "name cannot be empty");
    checkArgument(muleContext != null, "muleContext cannot be null");

    this.model = model;
    this.muleContext = muleContext;
  }

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

  /**
   * Builds a flow with the provided configuration.
   *
   * @return a new flow instance.
   */
  @Override
  public Operation build() {
    checkImmutable();

    operation = new DefaultOperation(model,
        muleContext,
        processors,
        maxConcurrency,
        createFlowStatistics(model, muleContext),
        OperationComponentInitialStateManager.INSTANCE);

    return operation;
  }

  private final void checkImmutable() {
    if (operation != null) {
      throw new IllegalStateException("Cannot change attributes once the operation was built");
    }
  }
}
