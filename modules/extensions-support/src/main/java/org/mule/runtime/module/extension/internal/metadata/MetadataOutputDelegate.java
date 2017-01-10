/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.metadata.api.utils.MetadataTypeUtils.isVoid;
import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.outputDescriptor;
import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.typeDescriptor;
import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_TYPE_AVAILABLE;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
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

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Metadata service delegate implementations that handles the resolution
 * of a {@link ComponentModel} {@link OutputMetadataDescriptor}
 *
 * @since 4.0
 */
class MetadataOutputDelegate extends BaseMetadataDelegate {

  MetadataOutputDelegate(ComponentModel componentModel) {
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
    MetadataResult<MetadataType> output = getOutputMetadata(context, key);
    MetadataResult<MetadataType> attributes = getOutputAttributesMetadata(context, key);

    MetadataResult<TypeMetadataDescriptor> outputDescriptor =
        toMetadataDescriptorResult(component.getOutput().getType(), component.getOutput().hasDynamicType(), output);
    MetadataResult<TypeMetadataDescriptor> attributesDescriptor =
        toMetadataDescriptorResult(component.getOutputAttributes().getType(), false, attributes);

    OutputMetadataDescriptor descriptor =
        outputDescriptor().withReturnType(outputDescriptor.get()).withAttributesType(attributesDescriptor.get()).build();

    if (!output.isSuccess() || !attributes.isSuccess()) {
      List<MetadataFailure> failures = ImmutableList.<MetadataFailure>builder()
          .addAll(output.getFailures())
          .addAll(attributes.getFailures())
          .build();

      return failure(descriptor, failures);
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
    try {
      MetadataType metadata = resolverFactory.getOutputResolver().getOutputType(context, key);
      if (isMetadataResolvedCorrectly(metadata, false)) {
        return success(metadata);
      }
      MetadataFailure failure = newFailure()
          .withMessage("Error resolving Output Payload metadata")
          .withFailureCode(NO_DYNAMIC_TYPE_AVAILABLE)
          .withReason(NULL_TYPE_ERROR)
          .onOutputPayload();
      return failure(output.getType(), failure);
    } catch (Exception e) {
      return failure(output.getType(), newFailure(e).onOutputAttributes());
    }
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
    try {
      MetadataType metadata = resolverFactory.getOutputAttributesResolver().getAttributesType(context, key);
      if (isMetadataResolvedCorrectly(metadata, false)) {
        return success(metadata);
      }
      MetadataFailure failure = newFailure()
          .withMessage("Error resolving Output Attributes metadata")
          .withFailureCode(NO_DYNAMIC_TYPE_AVAILABLE)
          .withReason(NULL_TYPE_ERROR)
          .onOutputAttributes();
      return failure(attributes.getType(), failure);
    } catch (Exception e) {
      return failure(attributes.getType(), newFailure(e).onOutputAttributes());
    }
  }

  private MetadataResult<TypeMetadataDescriptor> toMetadataDescriptorResult(MetadataType type,
                                                                            boolean isDynamic,
                                                                            MetadataResult<MetadataType> result) {
    MetadataType resultingType = result.get() == null ? type : result.get();
    TypeMetadataDescriptor descriptor = typeDescriptor()
        .withType(resultingType)
        .dynamic(isDynamic)
        .build();

    return result.isSuccess() ? success(descriptor) : failure(descriptor, result.getFailures());
  }
}
