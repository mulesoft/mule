/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.SOURCE_CALLBACK_CONTEXT_PARAM;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.execution.ReflectiveMethodComponentExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link SourceCallbackExecutor} which uses reflection to
 * execute the callback through a {@link Method}
 *
 * @since 4.0
 */
class ReflectiveSourceCallbackExecutor implements SourceCallbackExecutor {

  private final ExtensionModel extensionModel;
  private final Optional<ConfigurationInstance> configurationInstance;
  private final SourceModel sourceModel;
  private final MuleContext muleContext;
  private final ReflectiveMethodComponentExecutor<SourceModel> executor;

  /**
   * Creates a new instance
   *
   * @param extensionModel        the {@link ExtensionModel} of the owning component
   * @param configurationInstance an {@link Optional} {@link ConfigurationInstance} in case the component requires a config
   * @param sourceModel           the model of the {@code source}
   * @param source                a {@link Source} instance
   * @param method                the method to be executed
   * @param muleContext           the current {@link MuleContext}
   */
  public ReflectiveSourceCallbackExecutor(ExtensionModel extensionModel,
                                          Optional<ConfigurationInstance> configurationInstance,
                                          SourceModel sourceModel,
                                          Object source,
                                          Method method,
                                          MuleContext muleContext) {

    this.extensionModel = extensionModel;
    this.configurationInstance = configurationInstance;
    this.sourceModel = sourceModel;
    this.muleContext = muleContext;
    executor = new ReflectiveMethodComponentExecutor<>(sourceModel, method, source);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object execute(Event event, Map<String, Object> parameters, SourceCallbackContext context) throws Exception {
    return executor.execute(createExecutionContext(event, parameters, context));
  }

  private ExecutionContext<SourceModel> createExecutionContext(Event event, Map<String, Object> parameters,
                                                               SourceCallbackContext callbackContext) {
    ExecutionContextAdapter<SourceModel> executionContext = new DefaultExecutionContext<>(extensionModel,
                                                                                          configurationInstance,
                                                                                          parameters,
                                                                                          sourceModel,
                                                                                          event,
                                                                                          muleContext);

    executionContext.setVariable(SOURCE_CALLBACK_CONTEXT_PARAM, callbackContext);
    return executionContext;
  }
}
