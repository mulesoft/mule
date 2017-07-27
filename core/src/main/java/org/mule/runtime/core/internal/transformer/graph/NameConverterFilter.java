/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Filters a list of {@link Converter} returning a new list containing only the converters with the lower name using lexical
 * order.
 */
public class NameConverterFilter implements ConverterFilter {

  @Override
  public List<Converter> filter(List<Converter> converters, DataType source, DataType result) {
    if (converters.size() == 0) {
      return Collections.emptyList();
    }

    List<Converter> finteredConverters = new LinkedList<>(converters);
    sortConvertersByName(finteredConverters);

    String firstConverterName = finteredConverters.get(0).getName();
    int index = 1;
    for (; index < finteredConverters.size(); index++) {
      if (!firstConverterName.equals(finteredConverters.get(index).getName())) {
        break;
      }
    }

    return finteredConverters.subList(0, index);
  }

  private void sortConvertersByName(List<Converter> converterss) {
    Collections.sort(converterss, (converter, converter1) -> converter.getName().compareTo(converter1.getName()));
  }
}
