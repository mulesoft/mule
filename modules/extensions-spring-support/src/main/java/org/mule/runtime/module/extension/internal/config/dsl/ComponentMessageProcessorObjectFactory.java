/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;

/**
 * A base {@link AbstractExtensionObjectFactory} for producers of {@link ExtensionComponent} instances
 *
 * @since 4.0
 */
public abstract class ComponentMessageProcessorObjectFactory<M extends ComponentModel, P extends ExtensionComponent>
    extends AbstractExtensionObjectFactory<P> {

  protected final ExtensionModel extensionModel;
  protected final M componentModel;
  protected final PolicyManager policyManager;
  protected ConfigurationProvider configurationProvider;
  protected String target = EMPTY;
  protected String targetValue = PAYLOAD;
  protected CursorProviderFactory cursorProviderFactory;
  protected RetryPolicyTemplate retryPolicyTemplate;

  public ComponentMessageProcessorObjectFactory(ExtensionModel extensionModel,
                                                M componentModel,
                                                MuleContext muleContext,
                                                PolicyManager policyManager) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.componentModel = componentModel;
    this.policyManager = policyManager;
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
