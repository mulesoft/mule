/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.AbstractExecutionContextAdapterDecorator;

import java.util.Optional;

class PrecalculatedExecutionContextAdapter<M extends ComponentModel> extends AbstractExecutionContextAdapterDecorator<M> {

  private Optional<ConfigurationInstance> configuration;

  PrecalculatedExecutionContextAdapter(ExecutionContextAdapter<M> decorated) {
    super(decorated);

    configuration = decorated.getConfiguration();
  }

  @Override
  public Optional<ConfigurationInstance> getConfiguration() {
    return configuration;
  }
}
