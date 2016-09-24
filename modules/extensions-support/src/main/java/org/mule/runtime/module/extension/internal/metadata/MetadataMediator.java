/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.componentDescriptor;
import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.outputDescriptor;
import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.parameterDescriptor;
import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.typeDescriptor;
import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_TYPE_AVAILABLE;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.extension.api.util.NameUtils.getAliasName;
import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.isNullType;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.isVoid;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.subTypesUnion;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataKeysContainerBuilder;
import org.mule.runtime.api.metadata.MetadataProvider;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.builder.ComponentMetadataDescriptorBuilder;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.Named;
import org.mule.runtime.extension.api.introspection.OutputModel;
import org.mule.runtime.extension.api.introspection.RuntimeComponentModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.MetadataContentModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.util.SubTypesMappingContainer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Resolves a Component's Metadata by coordinating the several moving parts that are affected by the Metadata fetching process, so
 * that such pieces can remain decoupled.
 * <p/>
 * This mediator will coordinate the resolvers: {@link MetadataResolverFactory}, {@link MetadataKeysResolver},
 * {@link MetadataContentResolver} and {@link MetadataOutputResolver}, and the descriptors that represent their results:
 * {@link ComponentMetadataDescriptor}, {@link OutputMetadataDescriptor} and {@link TypeMetadataDescriptor}
 *
 * @since 4.0
 */
public class MetadataMediator {

  private final RuntimeComponentModel componentModel;
  private final MetadataResolverFactory resolverFactory;
  private final Optional<ParameterModel> contentParameter;
  private final List<ParameterModel> metadataKeyParts;
  private final SubTypesMappingContainer subTypesMappingContainer;
  private final MetadataKeyIdObjectResolver keyIdObjectResolver;

  public MetadataMediator(RuntimeExtensionModel extensionModel, RuntimeComponentModel componentModel) {
    this.componentModel = componentModel;
    this.resolverFactory = componentModel.getMetadataResolverFactory();
    this.contentParameter = getContentParameter(componentModel);
    this.metadataKeyParts = getMetadataKeyParts(componentModel);
    this.subTypesMappingContainer = getSubTypesMappingContainer(extensionModel);
    this.keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel, metadataKeyParts);
  }

  /**
   * Resolves the list of types available for the Content or Output of the associated {@link MetadataKeyProvider} Component,
   * representing them as a list of {@link MetadataKey}.
   * <p>
   * If no {@link MetadataKeyId} is present in the component's input parameters, then a {@link NullMetadataKey} is returned.
   * Otherwise, the {@link MetadataKeysResolver#getMetadataKeys} associated with the current Component will be invoked to obtain
   * the keys
   *
   * @param context current {@link MetadataContext} that will be used by the {@link MetadataKeysResolver}
   * @return Successful {@link MetadataResult} if the keys are obtained without errors Failure {@link MetadataResult} when no
   * Dynamic keys are a available or the retrieval fails for any reason
   */
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(MetadataContext context) {
    final String componentResolverName = getAliasName(componentModel.getMetadataResolverFactory().getClass());
    final MetadataKeysContainerBuilder keyBuilder = MetadataKeysContainerBuilder.getInstance();
    if (metadataKeyParts.isEmpty()) {
      return success(keyBuilder.add(componentResolverName, ImmutableSet.of(new NullMetadataKey())).build());
    }
    try {
      final Set<MetadataKey> metadataKeys = resolverFactory.getKeyResolver().getMetadataKeys(context);
      final Map<Integer, String> partOrder = getPartOrderMapping(metadataKeyParts);
      final Set<MetadataKey> enrichedMetadataKeys = metadataKeys.stream()
          .map(metadataKey -> cloneAndEnrichMetadataKey(metadataKey, partOrder, 1))
          .map(MetadataKeyBuilder::build).collect(toSet());
      keyBuilder.add(componentResolverName, enrichedMetadataKeys);
      return success(keyBuilder.build());
    } catch (Exception e) {
      return failure(e);
    }
  }

  /**
   * Resolves the {@link ComponentMetadataDescriptor} for the associated {@link MetadataProvider} without a key specified, this is
   * for the cases when the {@link ComponentModel} doesn't have a {@link MetadataKeyId} associated or the cases when the
   * {@link ComponentModel}s {@link MetadataKeyId} type has a default value to be built. (For multilevel {@link MetadataKey}s,
   * all the part members must have a default value)
   *
   * @return a successful {@link MetadataResult} of {@link ComponentMetadataDescriptor} with the Metadata representation of the
   * component, a failure {@link MetadataResult} if an error occur.
   */
  public MetadataResult<ComponentMetadataDescriptor> getMetadata(MetadataContext context) {
    try {
      if (metadataKeyParts.size() == 0) {
        return getMetadata(context, new NullMetadataKey());
      }
      Object resolvedKey = keyIdObjectResolver.resolve();
      return getMetadata(context, resolvedKey);
    } catch (MetadataResolvingException e) {
      return failure(e, e.getFailure());
    }
  }

  /**
   * Resolves the {@link ComponentMetadataDescriptor} for the associated {@link MetadataProvider} using the specified
   * {@link MetadataKey}
   * <p>
   * If Component's {@link Content} parameter has a {@link MetadataContentResolver} associated or its Output has a
   * {@link MetadataOutputResolver} associated that can be used to resolve dynamic {@link MetadataType}, then the
   * {@link ComponentMetadataDescriptor} will contain those Dynamic types instead of the static type declaration.
   *
   * @param context current {@link MetadataContext} that will be used by the metadata resolvers.
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved, used both for input and output types
   * @return Successful {@link MetadataResult} if the MetadataTypes are resolved without errors Failure {@link MetadataResult}
   * when the Metadata retrieval of any element fails for any reason
   */
  public MetadataResult<ComponentMetadataDescriptor> getMetadata(MetadataContext context, MetadataKey key) {
    try {
      Object resolvedKey = keyIdObjectResolver.resolve(key);
      return getMetadata(context, resolvedKey);
    } catch (MetadataResolvingException e) {
      return failure(e, e.getFailure());
    }
  }

  /**
   * Resolves the {@link ComponentMetadataDescriptor} for the associated {@link MetadataProvider} Component using static and
   * dynamic resolving of the Component parameters, attributes and output.
   * <p>
   * If Component's {@link Content} parameter has a {@link MetadataContentResolver} associated or its Output has a
   * {@link MetadataOutputResolver} associated that can be used to resolve dynamic {@link MetadataType}, then the
   * {@link ComponentMetadataDescriptor} will contain those Dynamic types instead of the static type declaration.
   *
   * @param context current {@link MetadataContext} that will be used by the {@link MetadataContentResolver} and
   *                {@link MetadataOutputResolver}
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved, used both for input and output types
   * @return Successful {@link MetadataResult} if the MetadataTypes are resolved without errors Failure {@link MetadataResult}
   * when the Metadata retrieval of any element fails for any reason
   */
  private MetadataResult<ComponentMetadataDescriptor> getMetadata(MetadataContext context, Object key) {
    MetadataResult<OutputMetadataDescriptor> outputResult = getOutputMetadataDescriptor(context, key);
    Optional<MetadataResult<ParameterMetadataDescriptor>> contentDescriptor = getContentMetadataDescriptor(context, key);

    ComponentMetadataDescriptorBuilder componentDescriptorBuilder = componentDescriptor(componentModel.getName())
        .withParametersDescriptor(getParametersMetadataDescriptors())
        .withOutputDescriptor(outputResult);

    if (!contentDescriptor.isPresent()) {
      return outputResult.isSuccess() ? success(componentDescriptorBuilder.build())
          : failure(componentDescriptorBuilder.build(), outputResult);
    }

    MetadataResult<ParameterMetadataDescriptor> contentResult = contentDescriptor.get();
    componentDescriptorBuilder.withContentDescriptor(contentResult);
    ComponentMetadataDescriptor componentMetadataDescriptor = componentDescriptorBuilder.build();

    if (!outputResult.isSuccess() || !contentResult.isSuccess()) {
      return mergeFailures(componentMetadataDescriptor, outputResult, contentResult);
    }

    return success(componentMetadataDescriptor);
  }

  /**
   * For each of the Component's {@link ParameterModel} creates the corresponding {@link TypeMetadataDescriptor} using only its
   * static {@link MetadataType} and ignoring if any parameter has a dynamic type.
   *
   * @return A {@link List} containing a {@link MetadataResult} of {@link TypeMetadataDescriptor} for each input parameter using
   * only its static {@link MetadataType} and ignoring if any parameter has a dynamic type.
   */
  private List<MetadataResult<ParameterMetadataDescriptor>> getParametersMetadataDescriptors() {
    Stream<ParameterModel> parameters = componentModel.getParameterModels().stream();

    if (contentParameter.isPresent()) {
      parameters = parameters.filter(p -> p != contentParameter.get());
    }

    return parameters.map(this::buildParameterTypeMetadataDescriptor).collect(new ImmutableListCollector<>());
  }

  /**
   * Builds a {@link TypeMetadataDescriptor} from the given {@link ParameterModel} an its subtypes if have any.
   *
   * @param parameterModel the {@link ParameterModel} to build the {@link TypeMetadataDescriptor}
   * @return a {@link MetadataResult} with the {@link TypeMetadataDescriptor} for the {@link ParameterModel}
   */
  private MetadataResult<ParameterMetadataDescriptor> buildParameterTypeMetadataDescriptor(ParameterModel parameterModel) {
    MetadataType metadataType = subTypesUnion(parameterModel.getType(), subTypesMappingContainer);
    return success(parameterDescriptor(parameterModel.getName()).withType(metadataType).build());
  }

  /**
   * Creates a {@link TypeMetadataDescriptor} representing the Component's Content metadata using the
   * {@link MetadataContentResolver}, if one is available to resolve the {@link MetadataType}. If no the Component has no Content
   * parameter, then {@link Optional#empty()} is returned.
   *
   * @param context current {@link MetadataContext} that will be used by the {@link MetadataContentResolver}
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return Success with an {@link Optional} {@link TypeMetadataDescriptor} representing the Component's Content metadata,
   * resolved using the {@link MetadataContentResolver} if one is available to resolve its {@link MetadataType}, returning
   * {@link Optional#empty()} if no Content parameter is present Failure if the dynamic resolution fails for any reason.
   */
  private Optional<MetadataResult<ParameterMetadataDescriptor>> getContentMetadataDescriptor(MetadataContext context,
                                                                                             Object key) {
    if (!contentParameter.isPresent()) {
      return Optional.empty();
    }

    MetadataResult<MetadataType> contentMetadataResult = getContentMetadata(context, key);
    ParameterMetadataDescriptor descriptor =
        parameterDescriptor(contentParameter.get().getName()).withType(contentMetadataResult.get()).build();

    return Optional.of(contentMetadataResult.isSuccess() ? success(descriptor) : failure(descriptor, contentMetadataResult));
  }

  /**
   * Creates an {@link OutputMetadataDescriptor} representing the Component's output metadata using the
   * {@link MetadataOutputResolver}, if one is available to resolve the output {@link MetadataType}.
   *
   * @param context current {@link MetadataContext} that will be used by the {@link MetadataContentResolver}
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return Success with an {@link OutputMetadataDescriptor} representing the Component's output metadata, resolved using the
   * {@link MetadataOutputResolver} if one is available to resolve its {@link MetadataType}. Failure if the dynamic
   * resolution fails for any reason.
   */
  private MetadataResult<OutputMetadataDescriptor> getOutputMetadataDescriptor(MetadataContext context, Object key) {
    MetadataResult<MetadataType> outputMetadataResult = getOutputMetadata(context, key);
    MetadataResult<MetadataType> attributesMetadataResult = getOutputAttributesMetadata(context, key);

    MetadataResult<TypeMetadataDescriptor> outputDescriptor = toTypeMetadataDescriptorResult(outputMetadataResult);
    MetadataResult<TypeMetadataDescriptor> attributesDescriptor = toTypeMetadataDescriptorResult(attributesMetadataResult);

    OutputMetadataDescriptor descriptor =
        outputDescriptor().withReturnType(outputDescriptor).withAttributesType(attributesDescriptor).build();

    if (!outputMetadataResult.isSuccess() || !attributesMetadataResult.isSuccess()) {
      return mergeFailures(descriptor, outputMetadataResult, attributesMetadataResult);
    }

    return success(descriptor);
  }

  private MetadataResult<TypeMetadataDescriptor> toTypeMetadataDescriptorResult(MetadataResult<MetadataType> result) {
    TypeMetadataDescriptor descriptor = typeDescriptor().withType(result.get()).build();
    if (result.isSuccess()) {
      return success(descriptor);
    }
    MetadataFailure failure = result.getFailure().get();
    return failure(descriptor, failure.getMessage(), failure.getFailureCode(), failure.getReason());
  }

  /**
   * Given a {@link MetadataKey} of a type and a {@link MetadataContext}, resolves the {@link MetadataType} of the {@link Content}
   * parameter using the {@link MetadataContentResolver} associated to the current component.
   *
   * @param context {@link MetadataContext} of the MetaData resolution
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return a success {@link MetadataResult} with the {@link MetadataType} of the {@link Content} parameter. A failure
   * {@link MetadataResult} if the component has no {@link Content} parameter
   */
  private MetadataResult<MetadataType> getContentMetadata(MetadataContext context, Object key) {
    if (!contentParameter.isPresent()) {
      return failure(null, "No @Content parameter found", NO_DYNAMIC_TYPE_AVAILABLE, "");
    }

    boolean allowsNullType = !contentParameter.get().isRequired() && (contentParameter.get().getDefaultValue() == null);
    return resolveMetadataType(allowsNullType,
                               subTypesUnion(contentParameter.get().getType(), subTypesMappingContainer),
                               () -> resolverFactory.getContentResolver().getContentMetadata(context, key),
                               contentParameter.get().getName());
  }

  /**
   * Given a {@link MetadataKey} of a type and a {@link MetadataContext}, resolves the {@link MetadataType} of the Components's
   * output using the {@link MetadataOutputResolver} associated to the current component.
   *
   * @param context {@link MetadataContext} of the Metadata resolution
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return a {@link MetadataResult} with the {@link MetadataType} of the component's output
   */
  private MetadataResult<MetadataType> getOutputMetadata(final MetadataContext context, final Object key) {
    OutputModel output = componentModel.getOutput();
    if (isVoid(output.getType()) || !output.hasDynamicType()) {
      return success(subTypesUnion(output.getType(), subTypesMappingContainer));
    }

    return resolveMetadataType(false, subTypesUnion(output.getType(), subTypesMappingContainer),
                               () -> resolverFactory.getOutputResolver().getOutputMetadata(context, key), "Output");
  }

  /**
   * Given a {@link MetadataKey} of a type and a {@link MetadataContext}, resolves the {@link MetadataType} of the Components's
   * output {@link Message#getAttributes()} using the {@link MetadataOutputResolver} associated to the current component.
   *
   * @param context {@link MetadataContext} of the Metadata resolution
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return a {@link MetadataResult} with the {@link MetadataType} of the components output {@link Message#getAttributes()}
   */
  private MetadataResult<MetadataType> getOutputAttributesMetadata(final MetadataContext context, Object key) {
    OutputModel attributes = componentModel.getOutputAttributes();
    if (isVoid(attributes.getType()) || !attributes.hasDynamicType()) {
      return success(subTypesUnion(attributes.getType(), subTypesMappingContainer));
    }

    return resolveMetadataType(false, subTypesUnion(attributes.getType(), subTypesMappingContainer),
                               () -> resolverFactory.getOutputAttributesResolver().getAttributesMetadata(context, key),
                               "OutputAttributes");
  }

  /**
   * Uses the {@link MetadataDelegate} to resolve dynamic metadata of the component, executing internally one of the
   * {@link MetadataType} resolving components: {@link MetadataContentResolver#getContentMetadata} or
   * {@link MetadataOutputResolver#getOutputMetadata}
   *
   * @param staticType static type used as default if no dynamic type is available
   * @param delegate   Delegate which performs the final invocation to the one of the metadata resolvers
   * @return a {@link MetadataResult} with the {@link MetadataType} resolved by the delegate invocation. Success if the type has
   * been successfully fetched, Failure otherwise.
   */
  private MetadataResult<MetadataType> resolveMetadataType(boolean allowsNullType, MetadataType staticType,
                                                           MetadataDelegate delegate, String elementName) {
    try {
      MetadataType dynamicType = delegate.resolve();
      // TODO review this once MULE-10438 and MDM-21 are done
      if (isNullType(staticType) || dynamicType == null) {
        return success(staticType);
      }

      if (isNullType(dynamicType) && !allowsNullType) {
        return failure(staticType, format("An error occurred while resolving the MetadataType of the [%s]", elementName),
                       NO_DYNAMIC_TYPE_AVAILABLE,
                       "The resulting MetadataType was of NullType, but it is not a valid type for this element");
      }

      return success(dynamicType);
    } catch (Exception e) {
      return failure(staticType, e);
    }
  }

  /**
   * Introspect the {@link List} of {@link ParameterModel} of the {@link ComponentModel} and filter the ones that are parts of the
   * {@link MetadataKey} and creates a mapping with the order number of each part with their correspondent name.
   *
   * @param parameterModels of the {@link ComponentModel}
   * @return the mapping of the order number of each part with their correspondent name
   */
  private Map<Integer, String> getPartOrderMapping(List<ParameterModel> parameterModels) {
    return parameterModels.stream().filter(part -> part.getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
        .collect(toMap(part -> part.getModelProperty(MetadataKeyPartModelProperty.class).get().getOrder(), Named::getName));
  }

  /**
   * Given a {@link MetadataKey}, this is navigated recursively cloning each {@link MetadataKey} of the tree structure creating a
   * {@link PartAwareMetadataKeyBuilder} and adding the partName of each {@link MetadataKey} found.
   *
   * @param key              {@link MetadataKey} to be cloned and enriched
   * @param partOrderMapping {@link Map} that contains the mapping of the name of each part of the {@link MetadataKey}
   * @param level            the current level of the part of the {@link MetadataKey} to be cloned and enriched
   * @return a {@link MetadataKeyBuilder} with the cloned and enriched keys
   */
  private MetadataKeyBuilder cloneAndEnrichMetadataKey(MetadataKey key, Map<Integer, String> partOrderMapping, int level) {
    final MetadataKeyBuilder keyBuilder = newKey(key.getId(), partOrderMapping.get(level)).withDisplayName(key.getDisplayName());
    key.getProperties().forEach(keyBuilder::withProperty);
    key.getChilds().forEach(childKey -> keyBuilder.withChild(cloneAndEnrichMetadataKey(childKey, partOrderMapping, level + 1)));
    return keyBuilder;
  }

  /**
   * @return the {@link SubTypesMappingContainer} associated to the extensionModel.
   */
  private SubTypesMappingContainer getSubTypesMappingContainer(ExtensionModel extensionModel) {
    return new SubTypesMappingContainer(extensionModel.getModelProperty(SubTypesModelProperty.class)
        .map(SubTypesModelProperty::getSubTypesMapping).orElse(ImmutableMap.of()));
  }

  /**
   * Merge multiple failed {@link MetadataResult} into one {@link MetadataFailure}.
   *
   * @param results the results to be merged as one
   * @return a new single {@link MetadataFailure}
   */
  private <T> MetadataResult<T> mergeFailures(T descriptor, MetadataResult<?>... results) {
    List<MetadataResult<?>> failedResults = Stream.of(results).filter(result -> !result.isSuccess()).collect(toList());
    String messages = failedResults.stream().map(f -> f.getFailure().get().getMessage()).collect(joining(" and "));
    String stackTrace = failedResults.size() == 1 ? failedResults.get(0).getFailure().get().getReason() : "";
    FailureCode failureCode =
        failedResults.size() == 1 ? failedResults.get(0).getFailure().get().getFailureCode() : FailureCode.MULTIPLE;
    return failure(descriptor, messages, failureCode, stackTrace);
  }

  @FunctionalInterface
  private interface MetadataDelegate {

    MetadataType resolve() throws MetadataResolvingException, ConnectionException;

  }

  private List<ParameterModel> getMetadataKeyParts(RuntimeComponentModel componentModel) {
    return componentModel.getParameterModels().stream()
        .filter(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
        .collect(toList());
  }

  private java.util.Optional<ParameterModel> getContentParameter(ComponentModel component) {
    return component.getParameterModels().stream()
        .filter(p -> p.getModelProperty(MetadataContentModelProperty.class).isPresent())
        .findFirst();
  }
}
