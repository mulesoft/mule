/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

public final class ConstructMessageProcessorBuilder
    extends ComponentMessageProcessorBuilder<ConstructModel, ConstructMessageProcessor> {

  public ConstructMessageProcessorBuilder(ExtensionModel extensionModel,
                                          ConstructModel operationModel,
                                          PolicyManager policyManager,
                                          MuleContext muleContext) {

    super(extensionModel, operationModel, policyManager, muleContext);
  }

  public ConstructMessageProcessor build() {
    return withContextClassLoader(getClassLoader(extensionModel), () -> {
      try {
        final ExtensionManager extensionManager = muleContext.getExtensionManager();
        final ResolverSet operationArguments = getArgumentsResolverSet();

        ConstructMessageProcessor processor = new ConstructMessageProcessor(extensionModel, operationModel,
                                                                            configurationProvider, target, targetValue,
                                                                            operationArguments,
                                                                            cursorProviderFactory, retryPolicyTemplate,
                                                                            extensionManager,
                                                                            policyManager);
        // TODO: MULE-5002 this should not be necessary but lifecycle issues when injecting message processors automatically
        muleContext.getInjector().inject(processor);
        return processor;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });
  }

}
