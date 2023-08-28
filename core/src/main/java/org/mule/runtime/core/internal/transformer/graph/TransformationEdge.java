/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.graph;

import org.mule.runtime.core.api.transformer.Converter;

/**
 * Represents an available transformation inside a transformation graph.
 */
class TransformationEdge {

  private final Converter converter;

  public TransformationEdge(Converter converter) {
    this.converter = converter;
  }

  public Converter getConverter() {
    return converter;
  }

  @Override
  public String toString() {
    return converter.getName();
  }
}
