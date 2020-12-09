/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.metadata;

import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataTypesDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.metadata.MetadataMediator;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.artifact.params.ExpressionNotSupportedException;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

public class MetadataComponentExecutor extends MetadataExecutor {

  public MetadataComponentExecutor(ConnectionManager connectionManager, ReflectionCache reflectionCache,
                                   ExpressionManager expressionManager, ArtifactHelper artifactHelper) {
    super(connectionManager, reflectionCache, expressionManager, artifactHelper);
    this.expressionManager = expressionManager;
  }

  public MetadataResult<ComponentMetadataTypesDescriptor> resolveComponentMetadata(ComponentModel componentModel,
                                                                                   ComponentElementDeclaration componentElementDeclaration) {
    try {
      Optional<ConfigurationInstance> optionalConfigurationInstance =
          getConfigurationInstance(componentModel, componentElementDeclaration);

      MetadataKeyResult metadataKeyResult =
          new MetadataKeyDeclarationResolver(componentModel, componentElementDeclaration, expressionManager).resolveKeyResult();
      if (!metadataKeyResult.isComplete()) {
        return failure(newFailure()
            .withMessage(metadataKeyResult.getPartialReason())
            .withFailureCode(INVALID_METADATA_KEY)
            .onComponent());
      }

      MetadataKey metadataKey = metadataKeyResult.getMetadataKey();
      ClassLoader extensionClassLoader = getClassLoader(artifactHelper.getExtensionModel(componentElementDeclaration));

      return resolveMetadata(componentModel, optionalConfigurationInstance, metadataKey, extensionClassLoader);
    } catch (ExpressionNotSupportedException e) {
      return failure(newFailure(e).withFailureCode(INVALID_METADATA_KEY).onKeys());
    } catch (MetadataResolvingException e) {
      return failure(newFailure(e).withFailureCode(e.getFailure()).onComponent());
    } catch (Exception e) {
      return failure(newFailure(e).onComponent());
    }
  }

  @Override
  protected boolean resolverRequiresConfiguration(Optional<TypeResolversInformationModelProperty> typeResolversInformationModelProperty,
                                                  ComponentModel componentModel) {
    //TODO MULE-15638 it is not correct the information provided by the TypeResolversInformationModelProperty model property
    //return typeResolversInformationModelProperty
    //.map(mp ->  {
    //  if (mp.getOutputResolver().map(resolverInformation -> resolverInformation.isRequiresConfiguration()).orElse(false)) {
    //    return true;
    //  }
    //  if (mp.getAttributesResolver().map(resolverInformation -> resolverInformation.isRequiresConfiguration()).orElse(false)) {
    //    return true;
    //  }
    //  return componentModel.getAllParameterModels().stream().map(parameter -> mp.getParameterResolver(parameter.getName())
    //          .map(resolverInformation -> resolverInformation.isRequiresConfiguration()).orElse(false)).findFirst().orElse(false);
    //}).orElse(false);
    return artifactHelper.hasParameterOfType(componentModel, CONFIG);
  }


  private MetadataResult<ComponentMetadataTypesDescriptor> resolveMetadata(ComponentModel componentModel,
                                                                           Optional<ConfigurationInstance> configurationInstance,
                                                                           MetadataKey metadataKey,
                                                                           ClassLoader extensionClassLoader) {
    MetadataMediator<? extends ComponentModel> metadataMediator = new MetadataMediator<>(componentModel);

    MetadataContext metadataContext = createMetadataContext(configurationInstance, extensionClassLoader);

    return withContextClassLoader(extensionClassLoader, () -> withMetadataContext(metadataContext, () -> {
      MetadataResult<InputMetadataDescriptor> inputMetadata = metadataMediator
          .getInputMetadata(metadataContext, metadataKey);
      MetadataResult<OutputMetadataDescriptor> outputMetadata = null;
      if (componentModel instanceof HasOutputModel) {
        outputMetadata = metadataMediator.getOutputMetadata(metadataContext, metadataKey);
      }
      return collectMetadata(inputMetadata, outputMetadata);
    }));
  }

  private MetadataResult<ComponentMetadataTypesDescriptor> collectMetadata(@Nonnull MetadataResult<InputMetadataDescriptor> inputMetadataResult,
                                                                           MetadataResult<OutputMetadataDescriptor> outputMetadataResult) {
    if (inputMetadataResult.isSuccess() && (outputMetadataResult == null || outputMetadataResult.isSuccess())) {
      ComponentMetadataTypesDescriptor.ComponentMetadataTypesDescriptorBuilder builder =
          ComponentMetadataTypesDescriptor.builder().withInputMetadataDescriptor(inputMetadataResult.get());
      if (outputMetadataResult != null) {
        builder.withOutputMetadataDescriptor(outputMetadataResult.get());
      }
      return success(builder.build());
    }
    List<MetadataFailure> failures = new ArrayList<>(inputMetadataResult.getFailures());
    if (outputMetadataResult != null) {
      failures.addAll(outputMetadataResult.getFailures());
    }
    return failure(failures);
  }

}
