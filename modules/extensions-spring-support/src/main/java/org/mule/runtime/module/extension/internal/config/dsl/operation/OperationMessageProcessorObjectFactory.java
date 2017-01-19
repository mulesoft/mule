/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static org.apache.commons.lang.StringUtils.EMPTY;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
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
  private ConfigurationProvider configuration;
  private String target = EMPTY;
  private CursorStreamProviderFactory cursorStreamProviderFactory;

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
        .setConfigurationProvider(configuration)
        .setParameters(parameters)
        .setTarget(target)
        .setCursorStreamProviderFactory(cursorStreamProviderFactory)
        .build();
  }

  public void setConfigurationProvider(ConfigurationProvider configuration) {
    this.configuration = configuration;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setCursorStreamProviderFactory(CursorStreamProviderFactory cursorStreamProviderFactory) {
    this.cursorStreamProviderFactory = cursorStreamProviderFactory;
  }
}
