/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthManager;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 *  Base class for creating MessageProcessor instances of a given {@link ComponentModel}
 *
 * @since 4.0
 */
public abstract class ComponentMessageProcessorBuilder<M extends ComponentModel, P extends ExtensionComponent> {

  protected final ExtensionModel extensionModel;
  protected final M operationModel;
  protected final PolicyManager policyManager;
  protected final MuleContext muleContext;
  protected Registry registry;
  protected final ExtensionConnectionSupplier extensionConnectionSupplier;
  protected final ExtensionsOAuthManager oauthManager;
  protected ConfigurationProvider configurationProvider;

  protected Map<String, ?> parameters;
  protected String target;
  protected String targetValue;
  protected CursorProviderFactory cursorProviderFactory;
  protected RetryPolicyTemplate retryPolicyTemplate;
  protected boolean lazyModeEnabled;

  public ComponentMessageProcessorBuilder(ExtensionModel extensionModel,
                                          M operationModel,
                                          PolicyManager policyManager,
                                          MuleContext muleContext,
                                          Registry registry) {

    checkArgument(extensionModel != null, "ExtensionModel cannot be null");
    checkArgument(operationModel != null, "OperationModel cannot be null");
    checkArgument(policyManager != null, "PolicyManager cannot be null");
    checkArgument(muleContext != null, "muleContext cannot be null");

    this.muleContext = muleContext;
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    this.policyManager = policyManager;
    this.registry = registry;
    this.extensionConnectionSupplier = registry.lookupByType(ExtensionConnectionSupplier.class).get();
    this.oauthManager = registry.lookupByType(ExtensionsOAuthManager.class).get();
  }

  public P build() {
    return withContextClassLoader(getClassLoader(extensionModel), () -> {
      try {
        final ExtensionManager extensionManager = muleContext.getExtensionManager();
        final ResolverSet operationArguments = getArgumentsResolverSet();

        P processor = createMessageProcessor(extensionManager, operationArguments);
        // TODO: MULE-5002 this should not be necessary but lifecycle issues when injecting message processors automatically
        muleContext.getInjector().inject(processor);
        return processor;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    });
  }

  protected abstract P createMessageProcessor(ExtensionManager extensionManager, ResolverSet operationArguments);

  protected ResolverSet getArgumentsResolverSet() throws ConfigurationException {
    final ResolverSet parametersResolverSet =
        ParametersResolver.fromValues(parameters, muleContext, lazyModeEnabled).getParametersAsResolverSet(operationModel,
                                                                                                           muleContext);

    final ResolverSet childsResolverSet =
        ParametersResolver.fromValues(parameters, muleContext, lazyModeEnabled).getNestedComponentsAsResolverSet(operationModel);

    return parametersResolverSet.merge(childsResolverSet);
  }

  public ComponentMessageProcessorBuilder<M, P> setConfigurationProvider(ConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
    return this;
  }

  public ComponentMessageProcessorBuilder<M, P> setParameters(Map<String, ?> parameters) {
    this.parameters = parameters != null ? parameters : new HashMap<>();
    return this;
  }

  public ComponentMessageProcessorBuilder<M, P> setTarget(String target) {
    this.target = target;
    return this;
  }

  public ComponentMessageProcessorBuilder<M, P> setTargetValue(String targetValue) {
    this.targetValue = targetValue;
    return this;
  }

  public ComponentMessageProcessorBuilder<M, P> setCursorProviderFactory(CursorProviderFactory cursorProviderFactory) {
    this.cursorProviderFactory = cursorProviderFactory;
    return this;
  }

  public ComponentMessageProcessorBuilder<M, P> setLazyMode(boolean lazyModeEnabled) {
    this.lazyModeEnabled = lazyModeEnabled;
    return this;
  }

  public ComponentMessageProcessorBuilder<M, P> setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate) {
    this.retryPolicyTemplate = retryPolicyTemplate;
    return this;
  }

  public ComponentMessageProcessorBuilder<M, P> setNestedProcessors(RetryPolicyTemplate retryPolicyTemplate) {
    this.retryPolicyTemplate = retryPolicyTemplate;
    return this;
  }

}
