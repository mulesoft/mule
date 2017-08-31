/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.operation;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
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
  private final OperationModel componentModel;
  private final PolicyManager policyManager;
  private ConfigurationProvider configurationProvider;
  private String target = EMPTY;
  private String targetValue = PAYLOAD;
  private CursorProviderFactory cursorProviderFactory;
  private RetryPolicyTemplate retryPolicyTemplate;

  public OperationMessageProcessorObjectFactory(ExtensionModel extensionModel,
                                                OperationModel componentModel,
                                                MuleContext muleContext,
                                                PolicyManager policyManager) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.componentModel = componentModel;
    this.policyManager = policyManager;
  }

  @Override
  public OperationMessageProcessor doGetObject() throws Exception {
    return new OperationMessageProcessorBuilder(extensionModel, componentModel, policyManager, muleContext)
        .setConfigurationProvider(configurationProvider)
        .setParameters(parameters)
        .setTarget(target)
        .setTargetValue(targetValue)
        .setCursorProviderFactory(cursorProviderFactory)
        .setRetryPolicyTemplate(retryPolicyTemplate)
        .build();
  }

  public void setConfigurationProvider(ConfigurationProvider configuration) {
    this.configurationProvider = configuration;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setTargetValue(String targetValue) {
    this.targetValue = targetValue;
  }

  public void setCursorProviderFactory(CursorProviderFactory cursorProviderFactory) {
    this.cursorProviderFactory = cursorProviderFactory;
  }

  public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate) {
    this.retryPolicyTemplate = retryPolicyTemplate;
  }
}
