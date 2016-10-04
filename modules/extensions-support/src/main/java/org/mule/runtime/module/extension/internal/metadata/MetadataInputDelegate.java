/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.parameterDescriptor;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.mergeResults;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.subTypesUnion;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.builder.InputMetadataDescriptorBuilder;
import org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.introspection.RuntimeComponentModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.util.SubTypesMappingContainer;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * //TODO
 */
class MetadataInputDelegate extends BaseMetadataDelegate {


  private final SubTypesMappingContainer typesMapping;

  MetadataInputDelegate(RuntimeComponentModel componentModel, SubTypesMappingContainer subTypesMappingContainer) {
    super(componentModel);
    typesMapping = subTypesMappingContainer;
  }

  /**
   * For each of the Component's {@link ParameterModel} creates the corresponding {@link TypeMetadataDescriptor} using only its
   * static {@link MetadataType} and ignoring if any parameter has a dynamic type.
   *
   * @return A {@link List} containing a {@link MetadataResult} of {@link TypeMetadataDescriptor} for each input parameter using
   * only its static {@link MetadataType} and ignoring if any parameter has a dynamic type.
   */
  MetadataResult<InputMetadataDescriptor> getInputMetadataDescriptors(MetadataContext context, Object key) {
    InputMetadataDescriptorBuilder input = MetadataDescriptorBuilder.inputDescriptor();

    List<MetadataResult<ParameterMetadataDescriptor>> results = new LinkedList<>();
    for (ParameterModel parameter : component.getParameterModels()) {
      MetadataResult<ParameterMetadataDescriptor> result = getParameterMetadataDescriptor(parameter, context, key);
      input.withParameter(parameter.getName(), result);
      results.add(result);
    }

    if (results.isEmpty()) {
      return success(input.build());
    }

    return mergeResults(input.build(), results.toArray(new MetadataResult<?>[] {}));
  }

  /**
   * Creates a {@link TypeMetadataDescriptor} representing the Component's Content metadata using the
   * {@link InputTypeResolver}, if one is available to resolve the {@link MetadataType}. If no the Component has no Content
   * parameter, then {@link Optional#empty()} is returned.
   *
   * @param context current {@link MetadataContext} that will be used by the {@link InputTypeResolver}
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return Success with an {@link Optional} {@link TypeMetadataDescriptor} representing the Component's Content metadata,
   * resolved using the {@link InputTypeResolver} if one is available to resolve its {@link MetadataType}, returning
   * {@link Optional#empty()} if no Content parameter is present Failure if the dynamic resolution fails for any reason.
   */
  private MetadataResult<ParameterMetadataDescriptor> getParameterMetadataDescriptor(ParameterModel parameter,
                                                                                     MetadataContext context, Object key) {

    MetadataResult<MetadataType> inputMetadataResult = getParameterMetadata(parameter, context, key);
    MetadataType type = inputMetadataResult.get() == null ? parameter.getType() : inputMetadataResult.get();
    ParameterMetadataDescriptor descriptor = parameterDescriptor(parameter.getName())
        .withType(type)
        .dynamic(!parameter.getType().equals(type))
        .build();

    return inputMetadataResult.isSuccess() ? success(descriptor) : failure(descriptor, inputMetadataResult);
  }

  /**
   * Given a {@link MetadataKey} of a type and a {@link MetadataContext}, resolves the {@link MetadataType} of the {@link Content}
   * parameter using the {@link InputTypeResolver} associated to the current component.
   *
   * @param context {@link MetadataContext} of the MetaData resolution
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return a success {@link MetadataResult} with the {@link MetadataType} of the {@link Content} parameter. A failure
   * {@link MetadataResult} if the component has no {@link Content} parameter
   */
  private MetadataResult<MetadataType> getParameterMetadata(ParameterModel parameter, MetadataContext context, Object key) {
    boolean allowsNullType = !parameter.isRequired() && (parameter.getDefaultValue() == null);
    return resolveMetadataType(allowsNullType, subTypesUnion(parameter.getType(), typesMapping),
                               () -> resolverFactory.getInputResolver(parameter.getName()).getInputMetadata(context, key),
                               parameter.getName());
  }
}
