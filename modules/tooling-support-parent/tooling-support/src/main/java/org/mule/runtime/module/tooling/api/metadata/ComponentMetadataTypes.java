/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.metadata;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Exposes all the dynamic metadata (input, output, attributes) of a given component.
 */
public class ComponentMetadataTypes {

  private Map<String, MetadataType> input = new HashMap<>();
  private MetadataType output;
  private MetadataType outputAttributes;

  public ComponentMetadataTypes(MetadataResult<InputMetadataDescriptor> inputMetadataResult,
                                MetadataResult<OutputMetadataDescriptor> outputMetadataResult) {
    requireNonNull(inputMetadataResult, "inputMetadataResult must not be null");
    if (!inputMetadataResult.isSuccess() || (outputMetadataResult != null && !outputMetadataResult.isSuccess())) {
      throw new IllegalStateException("Only successful metadata results can be provided");
    }

    inputMetadataResult.get().getAllParameters().values().stream()
        .filter(ParameterMetadataDescriptor::isDynamic).forEach(p -> input.put(p.getName(), p.getType()));
    if (outputMetadataResult != null) {
      TypeMetadataDescriptor payloadMetadata = outputMetadataResult.get().getPayloadMetadata();
      if (payloadMetadata.isDynamic()) {
        this.output = payloadMetadata.getType();
      }
      TypeMetadataDescriptor attributesMetadata = outputMetadataResult.get().getAttributesMetadata();
      if (attributesMetadata.isDynamic()) {
        this.outputAttributes = attributesMetadata.getType();
      }
    }
  }

  public Map<String, MetadataType> getInputMetadata() {
    return new HashMap<>(input);
  }

  public Optional<MetadataType> getInputMetadata(String parameter) {
    return ofNullable(input.get(parameter));
  }

  public Optional<MetadataType> getOutputMetadata() {
    return ofNullable(output);
  }

  public Optional<MetadataType> getOutputAttributesMetadata() {
    return ofNullable(outputAttributes);
  }

}
