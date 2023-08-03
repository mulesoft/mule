/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getPagingResultTransformer;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.supportsOAuth;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.GET_CONNECTION_SPAN_NAME;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingResultTransformer;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.List;

/**
 * Provides instances of {@link OperationMessageProcessor} for a given {@link OperationModel}
 *
 * @since 4.0
 */
public final class OperationMessageProcessorBuilder
    extends ComponentMessageProcessorBuilder<OperationModel, OperationMessageProcessor> {

  private final List<EnrichedErrorMapping> errorMappings;

  public OperationMessageProcessorBuilder(ExtensionModel extension,
                                          OperationModel operation,
                                          List<EnrichedErrorMapping> errorMappings,
                                          PolicyManager policyManager,
                                          MuleContext muleContext,
                                          Registry registry) {

    super(extension, operation, policyManager, registry.lookupByType(ReflectionCache.class).get(),
          registry.lookupByType(ExpressionManager.class).get(), muleContext, registry);

    this.errorMappings = errorMappings;
  }

  @Override
  protected OperationMessageProcessor createMessageProcessor(ExtensionManager extensionManager, ResolverSet arguments) {
    ValueResolver<ConfigurationProvider> configurationProviderResolver = getConfigurationProviderResolver();
    ResultTransformer resultTransformer = null;

    OperationMessageProcessor operationMessageProcessor = null;

    final boolean supportsOAuth = supportsOAuth(extensionModel);
    boolean isPagedOperation = operationModel.getModelProperty(PagedOperationModelProperty.class).isPresent();

    if (isPagedOperation) {
      resultTransformer = getPagingResultTransformer(operationModel, extensionConnectionSupplier, supportsOAuth)
          .orElse(null);
    }

    if (supportsOAuth) {
      operationMessageProcessor =
          new OAuthOperationMessageProcessor(extensionModel, operationModel, configurationProviderResolver, target,
                                             targetValue,
                                             errorMappings, arguments, cursorProviderFactory, retryPolicyTemplate,
                                             nestedChain, classLoader,
                                             extensionManager, policyManager, reflectionCache, resultTransformer,
                                             terminationTimeout);
    } else {
      operationMessageProcessor =
          new OperationMessageProcessor(extensionModel, operationModel, configurationProviderResolver, target, targetValue,
                                        errorMappings, arguments, cursorProviderFactory, retryPolicyTemplate, nestedChain,
                                        classLoader, extensionManager, policyManager, reflectionCache, resultTransformer,
                                        terminationTimeout);
    }

    // If case it is a paged operation, the initial span info is set so that it is not needed to be retrieved or calculated in
    // each request.
    if (isPagedOperation && resultTransformer != null) {
      ((PagingResultTransformer) resultTransformer).setOperationConnectionTracer(
                                                                                 componentTracerFactory
                                                                                     .fromComponent(operationMessageProcessor,
                                                                                                    GET_CONNECTION_SPAN_NAME,
                                                                                                    ""));
    }

    return operationMessageProcessor;
  }
}
