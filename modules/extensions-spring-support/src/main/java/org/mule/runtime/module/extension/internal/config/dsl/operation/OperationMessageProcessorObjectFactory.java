/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static java.util.Collections.emptyList;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.ComponentMessageProcessorObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorBuilder;

import java.util.List;

/**
 * An {@link AbstractExtensionObjectFactory} which produces {@link OperationMessageProcessor} instances
 *
 * @since 4.0
 */
public class OperationMessageProcessorObjectFactory
    extends ComponentMessageProcessorObjectFactory<OperationModel, OperationMessageProcessor> {

  private List<EnrichedErrorMapping> errorMappings = emptyList();

  public OperationMessageProcessorObjectFactory(ExtensionModel extensionModel,
                                                OperationModel componentModel,
                                                MuleContext muleContext,
                                                Registry registry,
                                                PolicyManager policyManager) {
    super(extensionModel,
          componentModel,
          muleContext,
          registry,
          policyManager);
  }

  @Override
  protected OperationMessageProcessorBuilder getMessageProcessorBuilder() {
    return new OperationMessageProcessorBuilder(extensionModel, componentModel, errorMappings, policyManager, muleContext,
                                                registry);
  }

  public void setErrorMappings(List<EnrichedErrorMapping> errorMappings) {
    this.errorMappings = errorMappings;
  }
}
