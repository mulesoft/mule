/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.strategy;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

/**
 * {@link OperationMessageProcessorStrategy} that creates a new {@link OperationMessageProcessor} for each execution.
 * 
 * @since 4.1.6
 */
public class NonCachedOperationMessageProcessorStrategy extends AbstractOperationMessageProcessorStrategy {

  /**
   * Creates a new instance
   */
  public NonCachedOperationMessageProcessorStrategy(ExtensionManager extensionManager, Registry registry, MuleContext muleContext,
                                                    PolicyManager policyManager, ReflectionCache reflectionCache,
                                                    CoreEvent event) {
    super(extensionManager, registry, muleContext, policyManager, reflectionCache, event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationMessageProcessor getOperationMessageProcessor(String extensionName, String operationName,
                                                                OperationParameters parameters) {
    return createProcessor(extensionName, operationName, parameters.getConfigName(),
                           resolveParameters(parameters.get(), getEvent(parameters)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CoreEvent getEvent(OperationParameters parameters) {
    return getBaseEvent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disposeProcessor(OperationMessageProcessor operationMessageProcessor) {
    OperationMessageProcessorUtils.disposeProcessor(operationMessageProcessor);
  }

}
