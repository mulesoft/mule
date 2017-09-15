/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.processor.ParametersResolverProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * An implementation of a {@link ComponentMessageProcessor} for {@link ConstructModel construct models}
 *
 * @since 4.0
 */
public class ConstructMessageProcessor extends ComponentMessageProcessor<ConstructModel>
    implements Processor, ParametersResolverProcessor<ConstructModel> {

  public ConstructMessageProcessor(ExtensionModel extensionModel,
                                   ConstructModel constructModel,
                                   ConfigurationProvider configurationProvider,
                                   String target,
                                   String targetValue,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   ExtensionManager extensionManager,
                                   PolicyManager policyManager) {
    super(extensionModel, constructModel, configurationProvider, target, targetValue,
          resolverSet, cursorProviderFactory, retryPolicyTemplate, extensionManager, policyManager);
  }

  @Override
  protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {
    // Constructs are config-less
  }

  @Override
  public ProcessingType getProcessingType() {
    // If processing type is CPU_LITE and operation is non-blocking then use CPU_LITE_ASYNC processing type so that the Flow can
    // return processing to a Flow thread.
    return CPU_LITE_ASYNC;
  }

}
