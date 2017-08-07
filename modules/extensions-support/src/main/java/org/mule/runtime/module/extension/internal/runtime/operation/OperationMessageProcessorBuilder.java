/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.supportsOAuth;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.TargetType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.HashMap;
import java.util.Map;

public final class OperationMessageProcessorBuilder {

  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;
  private final PolicyManager policyManager;
  private final MuleContext muleContext;
  private final ExtensionConnectionSupplier extensionConnectionSupplier;
  private final ExtensionsOAuthManager oauthManager;

  private ConfigurationProvider configurationProvider;
  private Map<String, ?> parameters;
  private String target;
  private TargetType targetType;
  private CursorProviderFactory cursorProviderFactory;

  public OperationMessageProcessorBuilder(ExtensionModel extensionModel,
                                          OperationModel operationModel,
                                          PolicyManager policyManager,
                                          MuleContext muleContext) {

    checkArgument(extensionModel != null, "ExtensionModel cannot be null");
    checkArgument(operationModel != null, "OperationModel cannot be null");
    checkArgument(policyManager != null, "PolicyManager cannot be null");
    checkArgument(muleContext != null, "muleContext cannot be null");

    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.policyManager = policyManager;
    this.muleContext = muleContext;
    extensionConnectionSupplier = lookup(ExtensionConnectionSupplier.class);
    oauthManager = lookup(ExtensionsOAuthManager.class);
  }

  private <T> T lookup(Class<T> type) {
    try {
      return muleContext.getRegistry().lookupObject(type);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(createStaticMessage(type.getName() + " Not Found"), e);
    }
  }

  public OperationMessageProcessorBuilder setConfigurationProvider(ConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
    return this;
  }

  public OperationMessageProcessorBuilder setParameters(Map<String, ?> parameters) {
    this.parameters = parameters != null ? parameters : new HashMap<>();
    return this;
  }

  public OperationMessageProcessorBuilder setTarget(String target) {
    this.target = target;
    return this;
  }

  public OperationMessageProcessorBuilder setTargetType(TargetType targetType) {
    this.targetType = targetType;
    return this;
  }

  public OperationMessageProcessorBuilder setCursorProviderFactory(CursorProviderFactory cursorProviderFactory) {
    this.cursorProviderFactory = cursorProviderFactory;
    return this;
  }

  public OperationMessageProcessor build() {
    return withContextClassLoader(getClassLoader(extensionModel), () -> {
      try {

        final ExtensionManager extensionManager = muleContext.getExtensionManager();
        final ResolverSet resolverSet =
            ParametersResolver.fromValues(parameters, muleContext).getParametersAsResolverSet(operationModel, muleContext);

        OperationMessageProcessor processor;
        if (operationModel.getModelProperty(PagedOperationModelProperty.class).isPresent()) {
          processor =
              new PagedOperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, targetType,
                                                 resolverSet,
                                                 cursorProviderFactory, extensionManager, policyManager,
                                                 extensionConnectionSupplier);
        } else if (supportsOAuth(extensionModel)) {
          processor =
              new OAuthOperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, targetType,
                                                 resolverSet,
                                                 cursorProviderFactory, extensionManager, policyManager,
                                                 oauthManager);
        } else {
          processor = new OperationMessageProcessor(extensionModel, operationModel, configurationProvider, target, targetType,
                                                    resolverSet,
                                                    cursorProviderFactory, extensionManager, policyManager);
        }
        // TODO: MULE-5002 this should not be necessary but lifecycle issues when injecting message processors automatically
        muleContext.getInjector().inject(processor);
        return processor;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });
  }
}
