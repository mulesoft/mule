/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.metadata.api.utils.MetadataTypeUtils.isCollection;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isNullType;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.metadata.resolving.FailureCode.CONNECTION_FAILURE;
import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_TYPE_AVAILABLE;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.module.extension.internal.metadata.MetadataResolverUtils.resolveWithOAuthRefresh;
import static org.mule.runtime.module.extension.internal.metadata.chain.NullChainInputTypeResolver.NULL_INSTANCE;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor.InputMetadataDescriptorBuilder;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor.ParameterMetadataDescriptorBuilder;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.module.extension.internal.metadata.chain.DefaultChainInputMetadataContext;
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Metadata service delegate implementations that handles the resolution of a {@link ComponentModel}
 * {@link InputMetadataDescriptor}
 *
 * @since 4.0
 */
class MetadataInputDelegate extends BaseMetadataDelegate {

  MetadataInputDelegate(EnrichableModel model) {
    super(model);
  }

  /**
   * For each of the Component's {@link ParameterModel} creates the corresponding {@link TypeMetadataDescriptor} using only its
   * static {@link MetadataType} and ignoring if any parameter has a dynamic type.
   *
   * @return A {@link List} containing a {@link MetadataResult} of {@link TypeMetadataDescriptor} for each input parameter using
   * only its static {@link MetadataType} and ignoring if any parameter has a dynamic type.
   */
  MetadataResult<InputMetadataDescriptor> getInputMetadataDescriptors(MetadataContext context, Object key) {
    if (!(model instanceof ParameterizedModel)) {
      return failure(MetadataFailure.Builder.newFailure()
          .withMessage("The given component has not parameter definitions to be described").onComponent());
    }
    InputMetadataDescriptorBuilder input = InputMetadataDescriptor.builder();
    List<MetadataResult<ParameterMetadataDescriptor>> results = new LinkedList<>();

    // Do this instead of {@link ParameterizedModel#getAllParameterModels() since for sources that
    // would merge the source parameters with the callback ones and that's not something we want here
    ((ParameterizedModel) model).getParameterGroupModels()
        .stream()
        .flatMap(parameterGroupModel -> parameterGroupModel.getParameterModels().stream())
        .forEach(parameter -> {
          MetadataResult<ParameterMetadataDescriptor> result = getParameterMetadataDescriptor(parameter, context, key);
          input.withParameter(parameter.getName(), result.get());
          results.add(result);
        });
    List<MetadataFailure> failures = results.stream().flatMap(e -> e.getFailures().stream()).collect(toList());
    return failures.isEmpty() ? success(input.build()) : failure(input.build(), failures);
  }

  /**
   * Resolves a {@link MessageMetadataType} that describes the message that will enter a scope's inner chain
   *
   * @param context                 the current {@link MetadataContext}
   * @param scopeInputMessageType   a {@link MessageMetadataType} for the message that originally entered the scope
   * @param inputMetadataDescriptor a previously resolved {@link InputMetadataDescriptor}
   * @return a {@link MetadataResult} with the resolved {@link MessageMetadataType}
   * @since 4.7.0
   */
  MetadataResult<MessageMetadataType> getScopeChainInputType(MetadataContext context,
                                                             Supplier<MessageMetadataType> scopeInputMessageType,
                                                             InputMetadataDescriptor inputMetadataDescriptor) {
    try {
      return resolveWithOAuthRefresh(context, () -> {
        ChainInputTypeResolver resolver = resolverFactory.getScopeChainInputTypeResolver().orElse(NULL_INSTANCE);
        ChainInputMetadataContext chainCtx =
            new DefaultChainInputMetadataContext(scopeInputMessageType, inputMetadataDescriptor, context);

        return success(resolver.getChainInputMetadataType(chainCtx));
      });
    } catch (ConnectionException e) {
      return connectivityFailure(e);
    } catch (Exception e) {
      return failure(newFailure(e).withMessage("Failed to resolve input types for scope inner chain").onComponent());
    }
  }

  /**
   * Resolves a {@link MessageMetadataType} that describes the message that will enter each route of a router component
   *
   * @param context                 the current {@link MetadataContext}
   * @param scopeInputMessageType   a {@link MessageMetadataType} for the message that originally entered the router
   * @param inputMetadataDescriptor a previously resolved {@link InputMetadataDescriptor}
   * @return a {@link MetadataResult} with the resolved {@link MessageMetadataType}
   * @since 4.7.0
   */
  MetadataResult<Map<String, MessageMetadataType>> getRoutesChainInputType(MetadataContext context,
                                                                           Supplier<MessageMetadataType> scopeInputMessageType,
                                                                           InputMetadataDescriptor inputMetadataDescriptor) {
    try {
      return resolveWithOAuthRefresh(context, () -> {
        ChainInputMetadataContext chainCtx =
            new DefaultChainInputMetadataContext(scopeInputMessageType, inputMetadataDescriptor, context);
        Map<String, MessageMetadataType> result = new HashMap<>();
        for (Map.Entry<String, ChainInputTypeResolver> entry : resolverFactory.getRouterChainInputResolvers().entrySet()) {
          try {
            result.put(entry.getKey(), entry.getValue().getChainInputMetadataType(chainCtx));
          } catch (ConnectionException e) {
            return connectivityFailure(e);
          } catch (Exception e) {
            return failure(newFailure(e).withMessage("Failed to resolve input types for route inner chain")
                .onParameter(entry.getKey()));
          }
        }
        return success(unmodifiableMap(result));
      });
    } catch (Exception e) {
      return failure(newFailure(e).withMessage(e.getMessage()).onComponent());
    }
  }


  private static <T> MetadataResult<T> connectivityFailure(ConnectionException e) {
    return failure(newFailure(e).withMessage("Failed to establish connection: " + getMessage(e))
        .withFailureCode(CONNECTION_FAILURE).onComponent());
  }

  /**
   * Given a parameters name, returns the associated {@link NamedTypeResolver}.
   *
   * @param parameterName name of the parameter
   * @return {@link NamedTypeResolver} of the parameter
   */
  NamedTypeResolver getParameterResolver(String parameterName) {
    return resolverFactory.getInputResolver(parameterName);
  }

  /**
   * Creates a {@link TypeMetadataDescriptor} representing the Component's Content metadata using the {@link InputTypeResolver},
   * if one is available to resolve the {@link MetadataType}. If no the Component has no Content parameter, then
   * {@link Optional#empty()} is returned.
   *
   * @param context current {@link MetadataContext} that will be used by the {@link InputTypeResolver}
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return Success with an {@link Optional} {@link TypeMetadataDescriptor} representing the Component's Content metadata,
   * resolved using the {@link InputTypeResolver} if one is available to resolve its {@link MetadataType}, returning
   * {@link Optional#empty()} if no Content parameter is present Failure if the dynamic resolution fails for any reason.
   */
  private MetadataResult<ParameterMetadataDescriptor> getParameterMetadataDescriptor(ParameterModel parameter,
                                                                                     MetadataContext context, Object key) {

    ParameterMetadataDescriptorBuilder descriptorBuilder = ParameterMetadataDescriptor.builder(parameter.getName());
    if (!parameter.hasDynamicType()) {
      return success(descriptorBuilder.withType(parameter.getType()).build());
    }

    descriptorBuilder.dynamic(true);
    MetadataResult<MetadataType> inputMetadataResult = getParameterMetadata(parameter, context, key);
    MetadataType type = inputMetadataResult.get() == null ? parameter.getType() : inputMetadataResult.get();
    ParameterMetadataDescriptor descriptor = descriptorBuilder.withType(type).build();
    return inputMetadataResult.isSuccess() ? success(descriptor) : failure(descriptor, inputMetadataResult.getFailures());
  }

  /**
   * Given a {@link MetadataKey} of a type and a {@link MetadataContext}, resolves the {@link MetadataType} of the
   * {@code parameter} using the {@link InputTypeResolver} associated to the current component.
   *
   * @param context {@link MetadataContext} of the MetaData resolution
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return a {@link MetadataResult} with the {@link MetadataType} of the {@code parameter}.
   */
  private MetadataResult<MetadataType> getParameterMetadata(ParameterModel parameter, MetadataContext context, Object key) {
    try {
      boolean allowsNullType = !parameter.isRequired() && (parameter.getDefaultValue() == null);
      MetadataType metadata =
          resolveWithOAuthRefresh(context,
              () -> resolverFactory.getInputResolver(parameter.getName()).getInputMetadata(context, key));
      if (isMetadataResolvedCorrectly(metadata, allowsNullType)) {
        return success(adaptToListIfNecessary(metadata, parameter, context));
      }
      MetadataFailure failure = newFailure()
          .withMessage(format("Error resolving metadata for the [%s] input parameter", parameter.getName()))
          .withFailureCode(NO_DYNAMIC_TYPE_AVAILABLE)
          .withReason(NULL_TYPE_ERROR)
          .onParameter(parameter.getName());
      return failure(parameter.getType(), failure);
    } catch (ConnectionException e) {
      return connectivityFailure(e);
    } catch (Exception e) {
      return failure(parameter.getType(), newFailure(e).onParameter(parameter.getName()));
    }
  }

  private MetadataType adaptToListIfNecessary(MetadataType resolvedType, ParameterModel parameter,
                                              MetadataContext metadataContext) {

    MetadataType inputType = parameter.getType();
    if (!isCollection(inputType) || isNullType(resolvedType)) {
      return resolvedType;
    }
    return metadataContext.getTypeBuilder().arrayType()
        .with(new ClassInformationAnnotation(getType(inputType)))
        .of(resolvedType)
        .build();
  }
}
