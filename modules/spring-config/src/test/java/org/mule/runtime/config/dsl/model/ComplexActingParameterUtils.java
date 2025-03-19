/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.core.api.util.func.CheckedConsumer;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class ComplexActingParameterUtils {

  static final ComplexActingParameter DEFAULT_COMPLEX_ACTING_PARAMETER;

  static {
    final int defaultInt = 0;
    final String defaultString = "zero";
    final List<String> defaultList = asList("one", "two", "three");
    final Map<String, String> defaultMap = ImmutableMap.of("0", "zero", "1", "one");

    final InnerPojo innerPojo = new InnerPojo(defaultInt, defaultString, defaultList, defaultMap);
    DEFAULT_COMPLEX_ACTING_PARAMETER = new ComplexActingParameter(defaultInt,
                                                                  defaultString,
                                                                  defaultList,
                                                                  defaultMap,
                                                                  innerPojo,
                                                                  singletonList(innerPojo),
                                                                  ImmutableMap.of(defaultString, innerPojo));
  }

  private ComplexActingParameterUtils() {}

  static ParameterValue declareComplexActingParameter(ComplexActingParameter complexActingParameter) {
    final int intParam = complexActingParameter.getIntParam();
    final String stringParam = complexActingParameter.getStringParam();
    final List<String> listParam = complexActingParameter.getListParam();
    final Map<String, String> mapParam = complexActingParameter.getMapParam();
    final InnerPojo innerPojo = complexActingParameter.getInnerPojoParam();
    final List<InnerPojo> complexList = complexActingParameter.getComplexListParam();
    final Map<String, InnerPojo> complexMap = complexActingParameter.getComplexMapParam();

    ParameterListValue.Builder listValueBuilder = ParameterListValue.builder();
    listParam.forEach(listValueBuilder::withValue);

    ParameterListValue.Builder complexListBuilder = ParameterListValue.builder();
    complexList.forEach(i -> complexListBuilder.withValue(declareInnerPojo(i)));

    ParameterObjectValue.Builder mapBuilder = ParameterObjectValue.builder();
    mapParam.forEach(mapBuilder::withParameter);

    ParameterObjectValue.Builder complexMapBuilder = ParameterObjectValue.builder();
    complexMap.forEach((k, v) -> complexMapBuilder.withParameter(k, declareInnerPojo(v)));

    return ParameterObjectValue.builder()
        .withParameter("innerPojoParam", declareInnerPojo(innerPojo))
        .withParameter("intParam", Integer.toString(intParam))
        .withParameter("stringParam", stringParam)
        .withParameter("listParam", listValueBuilder.build())
        .withParameter("mapParam", mapBuilder.build())
        .withParameter("complexListParam", complexListBuilder.build())
        .withParameter("complexMapParam", complexMapBuilder.build())
        .build();

  }

  /**
   * This function takes an {@link ArtifactDeclaration} for an already declared app and the location of a
   * {@link ComplexActingParameter}. Then it changes every field of the {@link ComplexActingParameter} and executes the consumer
   * for each new value.
   */
  static void forAllComplexActingParameterChanges(String componentLocation,
                                                  String complexActingParameterName,
                                                  CheckedConsumer<ComplexActingParameter> newValueConsumer) {

    final int defaultInt = DEFAULT_COMPLEX_ACTING_PARAMETER.getIntParam();
    final String defaultString = DEFAULT_COMPLEX_ACTING_PARAMETER.getStringParam();
    final List<String> defaultList = DEFAULT_COMPLEX_ACTING_PARAMETER.getListParam();
    final Map<String, String> defaultMap = DEFAULT_COMPLEX_ACTING_PARAMETER.getMapParam();
    final InnerPojo defaultInnerPojo = DEFAULT_COMPLEX_ACTING_PARAMETER.getInnerPojoParam();
    final List<InnerPojo> defaultComplexList = DEFAULT_COMPLEX_ACTING_PARAMETER.getComplexListParam();
    final Map<String, InnerPojo> defaultComplexMap = DEFAULT_COMPLEX_ACTING_PARAMETER.getComplexMapParam();

    ComplexActingParameter originalComplexActingParameter = new ComplexActingParameter(defaultInt,
                                                                                       defaultString,
                                                                                       defaultList,
                                                                                       defaultMap,
                                                                                       defaultInnerPojo,
                                                                                       defaultComplexList,
                                                                                       defaultComplexMap);

    newValueConsumer.accept(originalComplexActingParameter);
    newValueConsumer.accept(originalComplexActingParameter.copy().setIntParam(1));
    newValueConsumer.accept(originalComplexActingParameter.copy().setStringParam("one"));
    newValueConsumer.accept(originalComplexActingParameter.copy().setListParam(asList("one", "two", "four")));
    newValueConsumer.accept(originalComplexActingParameter.copy().setMapParam(ImmutableMap.of("2", "two",
                                                                                              "3", "three")));

    InnerPojo innerPojoChangedInt = new InnerPojo(1, defaultString, defaultList, defaultMap);
    InnerPojo innerPojoChangedString = new InnerPojo(defaultInt, "one", defaultList, defaultMap);
    InnerPojo innerPojoChangedList = new InnerPojo(defaultInt, defaultString, asList("one", "two", "four"), defaultMap);
    InnerPojo innerPojoChangedMap = new InnerPojo(defaultInt, defaultString, defaultList, ImmutableMap.of("0", "two",
                                                                                                          "1", "three"));

    newValueConsumer.accept(originalComplexActingParameter.copy().setInnerPojoParam(innerPojoChangedInt));
    newValueConsumer.accept(originalComplexActingParameter.copy().setInnerPojoParam(innerPojoChangedString));
    newValueConsumer.accept(originalComplexActingParameter.copy().setInnerPojoParam(innerPojoChangedList));
    newValueConsumer.accept(originalComplexActingParameter.copy().setInnerPojoParam(innerPojoChangedMap));

    newValueConsumer.accept(originalComplexActingParameter.copy().setComplexListParam(singletonList(innerPojoChangedInt)));
    newValueConsumer.accept(originalComplexActingParameter.copy().setComplexListParam(singletonList(innerPojoChangedString)));
    newValueConsumer.accept(originalComplexActingParameter.copy().setComplexListParam(singletonList(innerPojoChangedList)));
    newValueConsumer.accept(originalComplexActingParameter.copy().setComplexListParam(singletonList(innerPojoChangedMap)));

    newValueConsumer.accept(originalComplexActingParameter.copy().setComplexMapParam(ImmutableMap.of("0", innerPojoChangedInt)));
    newValueConsumer
        .accept(originalComplexActingParameter.copy().setComplexMapParam(ImmutableMap.of("0", innerPojoChangedString)));
    newValueConsumer.accept(originalComplexActingParameter.copy().setComplexMapParam(ImmutableMap.of("0", innerPojoChangedList)));
    newValueConsumer.accept(originalComplexActingParameter.copy().setComplexMapParam(ImmutableMap.of("0", innerPojoChangedMap)));
  }

  private static ParameterValue declareInnerPojo(InnerPojo innerPojo) {
    ParameterListValue.Builder listBuilder = ParameterListValue.builder();
    innerPojo.getListParam().forEach(listBuilder::withValue);

    ParameterObjectValue.Builder mapBuilder = ParameterObjectValue.builder();
    innerPojo.getMapParam().forEach(mapBuilder::withParameter);

    return ParameterObjectValue.builder()
        .ofType(InnerPojo.class.getName())
        .withParameter("intParam", Integer.toString(innerPojo.getIntParam()))
        .withParameter("stringParam", innerPojo.getStringParam())
        .withParameter("listParam", listBuilder.build())
        .withParameter("mapParam", mapBuilder.build())
        .build();
  }


}
