/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.typed.value.extension.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.vegan.extension.VeganProductInformation;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TypedValueParameterOperations {

  public static final String THIS_IS_A_DEFAULT_STRING = "This is a default string";

  @MediaType(TEXT_PLAIN)
  public TypedValue<String> typedValueOperationStringParameter(@Optional(
      defaultValue = THIS_IS_A_DEFAULT_STRING) TypedValue<String> stringValue, @Config TypedValueExtension config) {
    return stringValue;
  }

  public TypedValue<List<String>> typedValueOperationStringListParameter(TypedValue<List<String>> stringValues) {
    return stringValues;
  }

  public TypedValue<Map<String, String>> typedValueOperationStringMapParameter(TypedValue<Map<String, String>> stringMapValues) {
    return stringMapValues;
  }

  public TypedValue<KnockeableDoor> typedValueOperationDoorParameter(TypedValue<KnockeableDoor> doorValue) {
    return doorValue;
  }

  public TypedValue<List<KnockeableDoor>> typedValueOperationDoorListParameter(TypedValue<List<KnockeableDoor>> doorValues) {
    return doorValues;
  }

  public TypedValue<Map<String, KnockeableDoor>> typedValueOperationDoorMapParameter(TypedValue<Map<String, KnockeableDoor>> doorMapValues) {
    return doorMapValues;
  }

  public TypedValue<Map<String, List<String>>> typedValueOperationStringMapListParameter(TypedValue<Map<String, List<String>>> doorMapListValues) {
    return doorMapListValues;
  }

  public DifferedKnockableDoor typedValueForStringInsidePojo(DifferedKnockableDoor differedDoor) {
    return differedDoor;
  }

  public TypedValueExtension typedValueOnConfig(@Config TypedValueExtension extension) {
    return extension;
  }

  public VeganProductInformation typedValueOperationPojoWithNullsafeAndContent(@ParameterGroup(name = "param",
      showInDsl = true) VeganProductInformation param) {
    return param;
  }

  @MediaType(TEXT_PLAIN)
  public TypedValue<String> typedValueOperationWithStringContent(@Content TypedValue<String> stringDescription) {
    return stringDescription;
  }

  @OutputResolver(output = NullOutputResolver.class)
  public TypedValue<Object> typedValueForObject(@Content TypedValue<Object> objectTypedValue) {
    return objectTypedValue;
  }

  @MediaType(TEXT_PLAIN)
  public TypedValue<InputStream> typedValueForInputStream(@Content TypedValue<InputStream> inputStream) {
    return inputStream;
  }

  @OutputResolver(output = NullOutputResolver.class)
  public List<Object> mixedTypedValues(@ParameterGroup(name = "SimplePojo", showInDsl = true) SimplePojo pojo,
                                       @Optional String stringNotWrapped,
                                       @Optional TypedValue<String> wrappedString,
                                       @Optional @Content TypedValue<SimplePojo> complexTypedValue,
                                       @Optional @Content SimplePojo complexNotWrapped,
                                       @Optional @Content Map<String, Object> mapOfComplexValues,
                                       @Optional @Content(
                                           primary = true) Map<String, TypedValue<Object>> mapOfComplexTypedValues) {
    return Arrays.asList(stringNotWrapped, wrappedString, complexTypedValue, complexNotWrapped,
                         mapOfComplexValues, mapOfComplexTypedValues, pojo);

  }
}
