/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.internal.registry.TransformerWeighting;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Filters a list of {@link Converter} returning a new list containing only the converters with the highest priority weighting.
 */
public class PriorityWeightingConverterFilter implements ConverterFilter {

  @Override
  public List<Converter> filter(List<Converter> converters, DataType source, DataType result) {
    if (converters.size() == 0) {
      return Collections.emptyList();
    }

    List<TransformerWeighting> weightings = getTransformerWeightings(converters, source.getType(), result.getType());

    TransformerWeighting transformerWeighting = weightings.get(weightings.size() - 1);
    int index = weightings.size() - 2;
    List<Converter> heaviestConverter = new LinkedList<>();
    heaviestConverter.add((Converter) transformerWeighting.getTransformer());

    for (; index > -1; --index) {
      if (weightings.get(index).compareTo(transformerWeighting) < 0) {
        break;
      } else {
        heaviestConverter.add((Converter) weightings.get(index).getTransformer());
      }
    }

    return heaviestConverter;
  }

  private List<TransformerWeighting> getTransformerWeightings(List<Converter> converters, Class input, Class output) {
    List<TransformerWeighting> weightings = new LinkedList<>();
    for (Converter converter : converters) {
      TransformerWeighting current = new TransformerWeighting(input, output, converter);
      weightings.add(current);
    }
    Collections.sort(weightings);

    return weightings;
  }
}
