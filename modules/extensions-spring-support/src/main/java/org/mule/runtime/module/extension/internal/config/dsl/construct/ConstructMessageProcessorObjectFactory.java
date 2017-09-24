/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.construct;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.ComponentMessageProcessorObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.operation.ConstructMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.ConstructMessageProcessorBuilder;

/**
 * An {@link AbstractExtensionObjectFactory} which produces {@link ConstructMessageProcessor} instances
 *
 * @since 4.0
 */
public class ConstructMessageProcessorObjectFactory
    extends ComponentMessageProcessorObjectFactory<ConstructModel, ConstructMessageProcessor> {

  public ConstructMessageProcessorObjectFactory(ExtensionModel extensionModel,
                                                ConstructModel componentModel,
                                                MuleContext muleContext,
                                                Registry registry,
                                                PolicyManager policyManager) {
    super(extensionModel, componentModel, muleContext, registry, policyManager);
  }

  @Override
  protected ConstructMessageProcessorBuilder getMessageProcessorBuilder() {
    return new ConstructMessageProcessorBuilder(extensionModel, componentModel, policyManager, muleContext, registry);
  }

}
