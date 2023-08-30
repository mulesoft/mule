/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.runtime.client.NullComponent.NULL_COMPONENT;
import static org.mule.runtime.module.extension.internal.runtime.execution.CompletableOperationExecutorFactory.extractExecutorInitialisationParams;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getOperationExecutorFactory;

import static java.util.Optional.empty;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves the appropriate {@link CompletableComponentExecutor} for a given {@link OperationKey}
 *
 * @since 4.5.0
 */
abstract class ComponentExecutorResolver {

  /**
   * Factory method to obtain instances of this class
   */
  static CompletableComponentExecutor<OperationModel> from(OperationKey key,
                                                           ExtensionManager extensionManager,
                                                           ExpressionManager expressionManager,
                                                           ReflectionCache reflectionCache) {
    final OperationModel operationModel = key.getOperationModel();

    CompletableComponentExecutorFactory<OperationModel> operationExecutorFactory = getOperationExecutorFactory(operationModel);
    Map<String, Object> initParams = new HashMap<>();
    try {
      initParams.putAll(extractExecutorInitialisationParams(
                                                            key.getExtensionModel(),
                                                            operationModel,
                                                            initParams,
                                                            NULL_COMPONENT,
                                                            empty(),
                                                            extensionManager,
                                                            expressionManager,
                                                            reflectionCache));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(
                                                         "Exception found resolving parameters for operation client: "
                                                             + e.getMessage()),
                                     e);
    }

    return operationExecutorFactory.createExecutor(operationModel, initParams);
  }
}
