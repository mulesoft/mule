/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

/**
 * Converter from one type to another. Meant to be used for converting plain values from the mule configuration to specific types
 * required by a runtime object.
 *
 * @param <InputType> type of the value to be converted.
 * @param <OutputType> type of the converted value.
 */
public interface TypeConverter<InputType, OutputType> {

  /**
   * Converters from one type to another.
   *
   * @param inputType the value to be converted.
   * @return the converted value.
   */
  OutputType convert(InputType inputType);

}
