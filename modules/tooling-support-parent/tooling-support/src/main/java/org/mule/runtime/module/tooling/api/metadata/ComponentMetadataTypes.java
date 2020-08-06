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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Exposes all the dynamic metadata (input, output, attributes) of a given component.
 *
 * @since 4.4
 */
public class ComponentMetadataTypes {

  private final Map<String, MetadataType> input;
  private final MetadataType output;
  private final MetadataType outputAttributes;

  private ComponentMetadataTypes(Map<String, MetadataType> input, MetadataType output, MetadataType outputAttributes) {
    requireNonNull(input, "input metadata types map must not be null");
    this.input = input;
    this.output = output;
    this.outputAttributes = outputAttributes;
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

  public static class Builder {

    private InputMetadataDescriptor inputMetadataDescriptor;
    private OutputMetadataDescriptor outputMetadataDescriptor;

    public Builder withInputMetadataDescriptor(InputMetadataDescriptor inputMetadataDescriptor) {
      requireNonNull(inputMetadataDescriptor, "inputMetadataDescriptor must not be null");
      this.inputMetadataDescriptor = inputMetadataDescriptor;
      return this;
    }

    public Builder withOutputMetadataDescriptor(OutputMetadataDescriptor outputMetadataDescriptor) {
      requireNonNull(outputMetadataDescriptor, "outputMetadataDescriptor must not be null");
      this.outputMetadataDescriptor = outputMetadataDescriptor;
      return this;
    }

    public ComponentMetadataTypes build() {
      requireNonNull(inputMetadataDescriptor, "inputMetadataDescriptor must not be null");

      Map<String, MetadataType> input = new HashMap<>();
      MetadataType output = null;
      MetadataType outputAttributes = null;

      inputMetadataDescriptor.getAllParameters().values().stream()
          .filter(ParameterMetadataDescriptor::isDynamic).forEach(p -> input.put(p.getName(), p.getType()));
      if (outputMetadataDescriptor != null) {
        TypeMetadataDescriptor payloadMetadata = outputMetadataDescriptor.getPayloadMetadata();
        if (payloadMetadata.isDynamic()) {
          output = payloadMetadata.getType();
        }
        TypeMetadataDescriptor attributesMetadata = outputMetadataDescriptor.getAttributesMetadata();
        if (attributesMetadata.isDynamic()) {
          outputAttributes = attributesMetadata.getType();
        }
      }

      return new ComponentMetadataTypes(input, output, outputAttributes);
    }
  }
}
