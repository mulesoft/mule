/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.outputDescriptor;
import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.typeDescriptor;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.isVoid;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

/**
 * Metadata service delegate implementations that handles the resolution
 * of a {@link ComponentModel} {@link OutputMetadataDescriptor}
 *
 * @since 4.0
 */
class MetadataOutputDelegate extends BaseMetadataDelegate {

  public MetadataOutputDelegate(ComponentModel componentModel) {
    super(componentModel);
  }

  /**
   * Creates an {@link OutputMetadataDescriptor} representing the Component's output metadata using the
   * {@link OutputTypeResolver}, if one is available to resolve the output {@link MetadataType}.
   *
   * @param context current {@link MetadataContext} that will be used by the {@link InputTypeResolver}
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return Success with an {@link OutputMetadataDescriptor} representing the Component's output metadata, resolved using the
   * {@link OutputTypeResolver} if one is available to resolve its {@link MetadataType}. Failure if the dynamic
   * resolution fails for any reason.
   */
  MetadataResult<OutputMetadataDescriptor> getOutputMetadataDescriptor(MetadataContext context, Object key) {
    MetadataResult<MetadataType> outputMetadataResult = getOutputMetadata(context, key);
    MetadataResult<MetadataType> attributesMetadataResult = getOutputAttributesMetadata(context, key);

    MetadataResult<TypeMetadataDescriptor> outputDescriptor = toMetadataDescriptorResult(component.getOutput().getType(),
                                                                                         component.getOutput().hasDynamicType(),
                                                                                         outputMetadataResult);
    MetadataResult<TypeMetadataDescriptor> attributesDescriptor =
        toMetadataDescriptorResult(component.getOutputAttributes().getType(), false,
                                   attributesMetadataResult);

    OutputMetadataDescriptor descriptor =
        outputDescriptor().withReturnType(outputDescriptor).withAttributesType(attributesDescriptor).build();

    if (!outputMetadataResult.isSuccess() || !attributesMetadataResult.isSuccess()) {
      return mergeFailures(descriptor, outputMetadataResult, attributesMetadataResult);
    }

    return success(descriptor);
  }

  /**
   * Given a {@link MetadataKey} of a type and a {@link MetadataContext}, resolves the {@link MetadataType} of the Components's
   * output using the {@link OutputTypeResolver} associated to the current component.
   *
   * @param context {@link MetadataContext} of the Metadata resolution
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return a {@link MetadataResult} with the {@link MetadataType} of the component's output
   */
  private MetadataResult<MetadataType> getOutputMetadata(final MetadataContext context, final Object key) {
    OutputModel output = component.getOutput();
    if (isVoid(output.getType()) || !output.hasDynamicType()) {
      return success(output.getType());
    }

    return resolveMetadataType(false, output.getType(),
                               () -> resolverFactory.getOutputResolver().getOutputType(context, key), "Output");
  }

  /**
   * Given a {@link MetadataKey} of a type and a {@link MetadataContext}, resolves the {@link MetadataType} of the Components's
   * output {@link Message#getAttributes()} using the {@link OutputTypeResolver} associated to the current component.
   *
   * @param context {@link MetadataContext} of the Metadata resolution
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved
   * @return a {@link MetadataResult} with the {@link MetadataType} of the components output {@link Message#getAttributes()}
   */
  private MetadataResult<MetadataType> getOutputAttributesMetadata(final MetadataContext context, Object key) {
    OutputModel attributes = component.getOutputAttributes();
    if (isVoid(attributes.getType()) || !attributes.hasDynamicType()) {
      return success(attributes.getType());
    }

    return resolveMetadataType(false, attributes.getType(),
                               () -> resolverFactory.getOutputAttributesResolver().getAttributesType(context, key),
                               "OutputAttributes");
  }

  private MetadataResult<TypeMetadataDescriptor> toMetadataDescriptorResult(MetadataType type, boolean isDynamic,
                                                                            MetadataResult<MetadataType> result) {
    MetadataType resultingType = result.get() == null ? type : result.get();

    TypeMetadataDescriptor descriptor = typeDescriptor()
        .withType(resultingType)
        .dynamic(isDynamic)
        .build();

    if (result.isSuccess()) {
      return success(descriptor);
    }
    MetadataFailure failure = result.getFailure().get();
    return failure(descriptor, failure.getMessage(), failure.getFailureCode(), failure.getReason());
  }

}
