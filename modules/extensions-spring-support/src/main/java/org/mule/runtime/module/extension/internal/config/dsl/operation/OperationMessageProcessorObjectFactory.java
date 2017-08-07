/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mule.runtime.api.meta.TargetType.PAYLOAD;
import org.mule.runtime.api.meta.TargetType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessorBuilder;

/**
 * An {@link AbstractExtensionObjectFactory} which produces {@link OperationMessageProcessor} instances
 *
 * @since 4.0
 */
public class OperationMessageProcessorObjectFactory extends AbstractExtensionObjectFactory<OperationMessageProcessor> {

  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;
  private final PolicyManager policyManager;
  private ConfigurationProvider configurationProvider;
  private String target = EMPTY;
  private TargetType targetType = PAYLOAD;
  private CursorProviderFactory cursorProviderFactory;

  public OperationMessageProcessorObjectFactory(ExtensionModel extensionModel,
                                                OperationModel operationModel,
                                                MuleContext muleContext,
                                                PolicyManager policyManager) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.policyManager = policyManager;
  }

  @Override
  public OperationMessageProcessor doGetObject() throws Exception {
    return new OperationMessageProcessorBuilder(extensionModel, operationModel, policyManager, muleContext)
        .setConfigurationProvider(configurationProvider)
        .setParameters(parameters)
        .setTarget(target)
        .setTargetType(targetType)
        .setCursorProviderFactory(cursorProviderFactory)
        .build();
  }

  public void setConfigurationProvider(ConfigurationProvider configuration) {
    this.configurationProvider = configuration;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setTargetType(TargetType targetType) {
    this.targetType = targetType;
  }

  public void setCursorProviderFactory(CursorProviderFactory cursorProviderFactory) {
    this.cursorProviderFactory = cursorProviderFactory;
  }
}
