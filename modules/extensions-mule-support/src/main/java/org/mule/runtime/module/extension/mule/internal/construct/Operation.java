/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.construct;

import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

/**
 * A strongly typed operation defined through the Mule language.
 *
 * @since 4.5.0
 */
public interface Operation extends ExecutableComponent, Lifecycle {


  /**
   * @return the model that describes {@code this} operation
   */
  OperationModel getModel();

  /**
   * Creates instances of {@link Operation} with a default implementation
   * <p>
   * Builder instances can be configured until {@link #build()} is called. After that point, builder methods will fail to update
   * the builder state.
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
     * Configures the model that represents the produced operation
     *
     * @param operationModel an {@link OperationModel}
     * @return {@code this} builder
     */
    Builder setOperationModel(OperationModel operationModel);

    /**
     * Configures the {@link MuleContext} that owns the operation
     *
     * @param muleContext a {@link MuleContext}
     * @return {@code this} builder
     */
    Builder setMuleContext(MuleContext muleContext);

    /**
     * Builds an {@link Operation} with the provided configuration.
     *
     * @return a new {@link Operation} instance.
     * @throws IllegalStateException if operation model, processors or muleContext not set.
     */
    Operation build();
  }
}
