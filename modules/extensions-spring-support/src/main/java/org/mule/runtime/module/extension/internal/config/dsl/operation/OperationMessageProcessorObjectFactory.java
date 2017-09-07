/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.ComponentMessageProcessorObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ProcessorChainValueResolver;

import java.util.List;

/**
 * An {@link AbstractExtensionObjectFactory} which produces {@link OperationMessageProcessor} instances
 *
 * @since 4.0
 */
public class OperationMessageProcessorObjectFactory
    extends ComponentMessageProcessorObjectFactory<OperationModel, OperationMessageProcessor> {

  protected List<Processor> nestedProcessors;

  public OperationMessageProcessorObjectFactory(ExtensionModel extensionModel,
                                                OperationModel componentModel,
                                                MuleContext muleContext,
                                                PolicyManager policyManager) {
    super(extensionModel,
          componentModel,
          muleContext,
          policyManager);
  }

  @Override
  public OperationMessageProcessor doGetObject() throws Exception {
    if (nestedProcessors != null) {
      componentModel.getNestedComponents().stream()
          .filter(component -> component instanceof NestedChainModel)
          .findFirst()
          .ifPresent(chain -> parameters.put(chain.getName(), new ProcessorChainValueResolver(nestedProcessors)));
    }

    return new OperationMessageProcessorBuilder(extensionModel, componentModel, policyManager, muleContext)
        .setConfigurationProvider(configurationProvider)
        .setParameters(parameters)
        .setTarget(target)
        .setTargetValue(targetValue)
        .setCursorProviderFactory(cursorProviderFactory)
        .setRetryPolicyTemplate(retryPolicyTemplate)
        .build();
  }

  public void setNestedProcessors(List<Processor> nestedProcessors) {
    this.nestedProcessors = nestedProcessors;
  }

}
