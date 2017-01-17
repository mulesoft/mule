/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.loader.java.property.InterceptingModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.Map;

/**
 * Factory class, that returns instances of different {@link OperationMessageProcessor}s depending on the operation type.
 *
 * @since 4.0
 */
public class OperationMessageProcessorFactory {

  /**
   * Returns an {@link OperationMessageProcessor} implementation depending whether the operation is paginated, intercepting or a
   * regular operation.
   *
   * @return a new {@link OperationMessageProcessor} instance.
   */
  public static OperationMessageProcessor getOperationMessageProcessor(ExtensionModel extensionModel,
                                                                       OperationModel operationModel,
                                                                       ConfigurationProvider configurationProvider,
                                                                       PolicyManager policyManager,
                                                                       Map<String, ?> parameters,
                                                                       MuleContext muleContext,
                                                                       String target)
      throws Exception {
    return withContextClassLoader(getClassLoader(extensionModel), () -> {
      try {
        ResolverSet resolverSet = new ParametersResolver(muleContext, parameters).getParametersAsResolverSet(operationModel);
        OperationMessageProcessor processor;
        ExtensionManager extensionManager = muleContext.getExtensionManager();
        if (operationModel.getModelProperty(InterceptingModelProperty.class).isPresent()) {
          processor = new InterceptingOperationMessageProcessor(extensionModel, operationModel, configurationProvider, target,
                                                                resolverSet, extensionManager, policyManager);
        } else if (operationModel.getModelProperty(PagedOperationModelProperty.class).isPresent()) {
          processor =
              new PagedOperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, resolverSet,
                                                 extensionManager, policyManager);
        } else {
          processor = new OperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, resolverSet,
                                                    extensionManager, policyManager);
        }
        // TODO: MULE-5002 this should not be necessary but lifecycle issues when injecting message processors automatically
        muleContext.getInjector().inject(processor);
        return processor;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });
  }
}
