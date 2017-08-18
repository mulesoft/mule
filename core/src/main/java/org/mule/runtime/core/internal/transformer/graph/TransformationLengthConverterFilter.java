/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.privileged.transformer.CompositeConverter;

import java.util.Collections;
import java.util.List;

/**
 * Filters a list of {@link Converter} returning a new list containing only the converters with the lower transformation path
 * length.
 */
public class TransformationLengthConverterFilter implements ConverterFilter {

  @Override
  public List<Converter> filter(List<Converter> converters, DataType source, DataType result) {
    if (converters.size() == 0) {
      return Collections.emptyList();
    }

    sortByTransformationLength(converters);

    int shortestLength = getTransformationLength(converters.get(0));
    int index = 1;
    for (; index < converters.size(); index++) {
      if (getTransformationLength(converters.get(index)) > shortestLength) {
        break;
      }
    }

    return converters.subList(0, index);
  }

  private void sortByTransformationLength(List<Converter> converterss) {
    Collections.sort(converterss, (converter, converter1) -> {
      int length = getTransformationLength(converter);
      int length1 = getTransformationLength(converter1);

      return length - length1;
    });
  }

  private int getTransformationLength(Converter converter) {
    if (converter instanceof CompositeConverter) {
      return ((CompositeConverter) converter).getConverters().size();
    } else {
      return 1;
    }
  }
}
