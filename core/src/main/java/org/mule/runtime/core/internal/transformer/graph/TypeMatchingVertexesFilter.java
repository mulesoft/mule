/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;

import java.util.List;
import java.util.function.Predicate;

/**
 * Filters found converters returning the ones that match the source or result {@link DataType} exactly or all of them if none
 * match.
 *
 * @since 4.0
 */
public class TypeMatchingVertexesFilter implements ConverterFilter {

  @Override
  public List<Converter> filter(List<Converter> converters, DataType source, DataType result) {
    List<Converter> filteredByType = filterByResultType(filterBySourceType(converters, source), result);
    if (!ANY.matches(result.getMediaType())) {
      return filterByResultMimeType(filteredByType, result);
    }
    return filteredByType;
  }

  private List<Converter> filterBySourceType(List<Converter> converters, DataType source) {
    return filterBySource(converters, dataType -> dataType.getType().equals(source.getType()));
  }

  private List<Converter> filterByResultType(List<Converter> converters, DataType result) {
    return filterByResult(converters, converter -> converter.getReturnDataType().getType().equals(result.getType()));
  }

  private List<Converter> filterByResultMimeType(List<Converter> converters, DataType result) {
    return filterByResult(converters, converter -> converter.getReturnDataType().getMediaType().matches(result.getMediaType()));
  }

  private List<Converter> filterBySource(List<Converter> converters, Predicate<DataType> matcher) {
    List<Converter> filteredConvertersBySource =
        converters.stream().filter(converter -> converter.getSourceDataTypes().stream().anyMatch(matcher)).collect(toList());

    if (filteredConvertersBySource.isEmpty()) {
      filteredConvertersBySource = converters;
    }

    return filteredConvertersBySource;
  }

  private List<Converter> filterByResult(List<Converter> converters, Predicate<Converter> matcher) {
    List<Converter> filteredConvertersByResult = converters.stream().filter(matcher).collect(toList());

    if (filteredConvertersByResult.isEmpty()) {
      filteredConvertersByResult = converters;
    }

    return filteredConvertersByResult;
  }

}
