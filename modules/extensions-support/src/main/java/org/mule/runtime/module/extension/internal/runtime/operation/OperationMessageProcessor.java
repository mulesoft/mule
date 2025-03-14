/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_POLICY_ISOLATION;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.runtime.ExecutionTypeMapper.asProcessingType;

import static java.lang.String.format;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.EntityMetadataProvider;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.core.internal.exception.ErrorMappingsAware;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.metadata.EntityMetadataMediator;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.List;

import jakarta.inject.Inject;

/**
 * An implementation of a {@link ComponentMessageProcessor} for {@link OperationModel operation models}
 *
 * @since 3.7.0
 */
public class OperationMessageProcessor extends ComponentMessageProcessor<OperationModel>
    implements EntityMetadataProvider, ErrorMappingsAware {

  static final String INVALID_TARGET_MESSAGE =
      "Root component '%s' defines an invalid usage of operation '%s' which uses %s as %s";
  private EntityMetadataMediator entityMetadataMediator;

  private final List<EnrichedErrorMapping> errorMappings;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  public OperationMessageProcessor(ExtensionModel extensionModel,
                                   OperationModel operationModel,
                                   ValueResolver<ConfigurationProvider> configurationProviderResolver,
                                   String target,
                                   String targetValue,
                                   List<EnrichedErrorMapping> errorMappings,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   MessageProcessorChain nestedChain,
                                   ClassLoader classLoader,
                                   ExtensionManager extensionManager,
                                   ReflectionCache reflectionCache) {
    this(extensionModel, operationModel, configurationProviderResolver, target, targetValue, errorMappings, resolverSet,
         cursorProviderFactory, retryPolicyTemplate, nestedChain, classLoader,
         extensionManager, reflectionCache, null, -1);
  }

  public OperationMessageProcessor(ExtensionModel extensionModel,
                                   OperationModel operationModel,
                                   ValueResolver<ConfigurationProvider> configurationProviderResolver,
                                   String target,
                                   String targetValue,
                                   List<EnrichedErrorMapping> errorMappings,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   MessageProcessorChain nestedChain,
                                   ClassLoader classLoader,
                                   ExtensionManager extensionManager,
                                   ReflectionCache reflectionCache,
                                   ResultTransformer resultTransformer,
                                   long terminationTimeout) {
    super(extensionModel, operationModel, configurationProviderResolver, target, targetValue, resolverSet,
          cursorProviderFactory, retryPolicyTemplate, nestedChain, classLoader,
          extensionManager, reflectionCache, resultTransformer, terminationTimeout);
    this.errorMappings = errorMappings;
  }

  /**
   * Validates that the {@link #componentModel} is valid for the given {@code configurationProvider}
   *
   * @throws IllegalOperationException If the validation fails
   */
  @Override
  protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {
    validatePolicyIsolation(configurationProvider);
    ConfigurationModel configurationModel = configurationProvider.getConfigurationModel();
    if (!configurationModel.getOperationModel(componentModel.getName()).isPresent() &&
        !configurationProvider.getExtensionModel().getOperationModel(componentModel.getName()).isPresent()) {
      throw new IllegalOperationException(format(
                                                 "Root component '%s' defines an usage of operation '%s' which points to configuration '%s'. "
                                                     + "The selected config does not support that operation.",
                                                 getLocation().getRootContainerName(), componentModel.getName(),
                                                 configurationProvider.getName()));
    }
  }

  // TODO MULE-19350 move this validation to the artifact AST construction
  private void validatePolicyIsolation(ConfigurationProvider configurationProvider) {
    if (featureFlaggingService.isEnabled(ENABLE_POLICY_ISOLATION)
        && muleContext.getArtifactType().equals(POLICY)
        && configurationProvider.getExtensionModel() != extensionModel) {
      throw new IllegalOperationException(format(
                                                 "Root component '%s' defines an usage of operation '%s' which points to configuration '%s'. "
                                                     + "The selected config is not part of the policy declaration and cannot be shared.",
                                                 getLocation().getRootContainerName(), componentModel.getName(),
                                                 configurationProvider.getName()));
    }
  }

  @Override
  public ProcessingType getInnerProcessingType() {
    ProcessingType processingType = asProcessingType(componentModel.getExecutionType());
    if (processingType == CPU_LITE && !componentModel.isBlocking()) {
      // If processing type is CPU_LITE and operation is non-blocking then use CPU_LITE_ASYNC processing type so that the Flow can
      // return processing to a Flow thread.
      return CPU_LITE_ASYNC;
    } else {
      return processingType;
    }
  }

  @Override
  protected boolean mayCompleteInDifferentThread() {
    // If the operation is non blocking we can guarantee that that the processor will jump threads.
    if (!isBlocking()) {
      return true;
    }

    return super.mayCompleteInDifferentThread();
  }

  @Override
  protected boolean isBlocking() {
    return componentModel.isBlocking();
  }

  @Override
  public List<EnrichedErrorMapping> getErrorMappings() {
    return errorMappings;
  }

  /////////////////////////////////////////////////////////////////////////////
  // "Fat" Tooling support
  /////////////////////////////////////////////////////////////////////////////

  @Override
  protected void initializeForFatTooling() {
    this.entityMetadataMediator = new EntityMetadataMediator(componentModel);
    super.initializeForFatTooling();
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getEntityKeys() throws MetadataResolvingException {
    try {
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader,
                                                                      () -> entityMetadataMediator.getEntityKeys(context)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onKeys());
    }
  }

  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(MetadataKey key) throws MetadataResolvingException {
    try {
      return runWithMetadataContext(
                                    context -> withContextClassLoader(classLoader, () -> entityMetadataMediator
                                        .getEntityMetadata(context, key)));
    } catch (ConnectionException e) {
      return failure(newFailure(e).onKeys());
    }
  }

}
