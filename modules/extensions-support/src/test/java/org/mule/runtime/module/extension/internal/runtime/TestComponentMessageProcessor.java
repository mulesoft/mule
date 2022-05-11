/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.operation.ComponentMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.DefaultExecutionMediator.ResultTransformer;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

public abstract class TestComponentMessageProcessor extends ComponentMessageProcessor<ComponentModel> {

  public TestComponentMessageProcessor(ExtensionModel extensionModel, ComponentModel componentModel,
                                       ConfigurationProvider configurationProvider, String target, String targetValue,
                                       ResolverSet resolverSet, CursorProviderFactory cursorProviderFactory,
                                       RetryPolicyTemplate retryPolicyTemplate, MessageProcessorChain nestedChain,
                                       ExtensionManager extensionManager, PolicyManager policyManager,
                                       ReflectionCache reflectionCache, ResultTransformer resultTransformer,
                                       long terminationTimeout) {
    super(extensionModel, componentModel, configurationProvider, target, targetValue, resolverSet, cursorProviderFactory,
          retryPolicyTemplate, nestedChain, extensionManager, policyManager, reflectionCache, resultTransformer,
          terminationTimeout);
  }
}
