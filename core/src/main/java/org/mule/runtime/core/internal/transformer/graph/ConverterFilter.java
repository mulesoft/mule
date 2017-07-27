/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;

import java.util.List;

/**
 * Provides a way to filter a converters during the process of transformer resolution for a given source and result data types.
 */
public interface ConverterFilter {

  /**
   * Filters a list of converters
   *
   * @param converters converters to filter
   * @param source source data type being resolved
   * @param result result data type being resolved
   * @return a non null list of converters that match the specified criteria
   */
  public List<Converter> filter(List<Converter> converters, DataType source, DataType result);

}
