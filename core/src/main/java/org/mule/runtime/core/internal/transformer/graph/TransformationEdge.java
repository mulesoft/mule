/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
