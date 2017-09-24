/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Provides instances of {@link ConstructMessageProcessor} for a given {@link ConstructModel}
 *
 * @since 4.0
 */
public final class ConstructMessageProcessorBuilder
    extends ComponentMessageProcessorBuilder<ConstructModel, ConstructMessageProcessor> {

  public ConstructMessageProcessorBuilder(ExtensionModel extensionModel,
                                          ConstructModel operationModel,
                                          PolicyManager policyManager,
                                          MuleContext muleContext,
                                          Registry registry) {

    super(extensionModel, operationModel, policyManager, muleContext, registry);
  }

  @Override
  protected ConstructMessageProcessor createMessageProcessor(ExtensionManager extensionManager, ResolverSet arguments) {
    return new ConstructMessageProcessor(extensionModel, operationModel,
                                         configurationProvider, target, targetValue,
                                         arguments,
                                         cursorProviderFactory, retryPolicyTemplate,
                                         extensionManager,
                                         policyManager);
  }

}
