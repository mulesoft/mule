/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.construct.operation.DefaultOperationBuilder;

import java.util.List;

public interface Operation extends Lifecycle, Processor {

  /**
   * Creates a new operation builder
   *
   * @param model       The {@link OperationModel} for this operation
   * @param muleContext context where the flow will be registered.
   */
  static Builder builder(OperationModel model, MuleContext muleContext) {
    return new DefaultOperationBuilder(model, muleContext);
  }

  OperationModel getModel();

  /**
   * Creates instances of {@link Operation} with a default implementation
   * <p>
   * Builder instances can be configured until {@link #build()} is called. After that point,
   * builder methods will fail to update the builder state.
   *
   * @since 4.5.0
   */
  interface Builder {

    /**
     * Configures the message processors to execute as part of the operation.
     *
     * @param processors message processors to execute. Non null.
     * @return same builder instance.
     */
    Builder processors(List<Processor> processors);

    /**
     * Configures the message processors to execute as part of the operation.
     *
     * @param processors message processors to execute.
     * @return {@code this} builder
     */
    Builder processors(Processor... processors);


    /**
     * Builds an {@link Operation} with the provided configuration.
     *
     * @return a new {@link Operation} instance.
     */
    Operation build();
  }
}
