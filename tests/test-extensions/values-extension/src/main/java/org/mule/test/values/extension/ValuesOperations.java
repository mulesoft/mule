/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.sdk.api.values.FieldValues;
import org.mule.test.values.extension.metadata.JsonTypeResolver;
import org.mule.test.values.extension.metadata.XmlTypeResolver;
import org.mule.test.values.extension.resolver.MultiLevelValueProvider;
import org.mule.test.values.extension.resolver.SimpleValueProvider;
import org.mule.test.values.extension.resolver.TrueFalseValueProvider;
import org.mule.test.values.extension.resolver.WithComplexActingParameter;
import org.mule.test.values.extension.resolver.WithConfigValueProvider;
import org.mule.test.values.extension.resolver.WithConnectionValueProvider;
import org.mule.test.values.extension.resolver.WithEnumParameterValueProvider;
import org.mule.test.values.extension.resolver.WithErrorValueProvider;
import org.mule.test.values.extension.resolver.WithFourActingParametersValueProvider;
import org.mule.test.values.extension.resolver.WithListParameterValueProvider;
import org.mule.test.values.extension.resolver.WithMapParameterValueProvider;
import org.mule.test.values.extension.resolver.WithMuleContextValueProvider;
import org.mule.test.values.extension.resolver.WithOptionalParameterSdkValueProvider;
import org.mule.test.values.extension.resolver.WithOptionalParametersValueProvider;
import org.mule.test.values.extension.resolver.WithOptionalParametersWithDefaultValueProvider;
import org.mule.test.values.extension.resolver.WithPojoParameterValueProvider;
import org.mule.test.values.extension.resolver.WithRequiredAndOptionalParametersValueProvider;
import org.mule.test.values.extension.resolver.WithRequiredParameterFromGroupValueProvider;
import org.mule.test.values.extension.resolver.WithRequiredParameterSdkValueProvider;
import org.mule.test.values.extension.resolver.WithRequiredParameterValueProvider;
import org.mule.test.values.extension.resolver.WithRequiredParametersValueProvider;
import org.mule.test.values.extension.resolver.WithTwoActingParametersValueProvider;

import java.io.InputStream;
import java.util.List;

public class ValuesOperations {

  public void singleValuesEnabledParameter(@org.mule.sdk.api.annotation.values.OfValues(SimpleValueProvider.class) String channels) {

  }

  public void singleValuesEnabledParameterWithConnection(@org.mule.sdk.api.annotation.values.OfValues(WithConnectionValueProvider.class) String channels,
                                                         @Connection ValuesConnection connection) {}

  public void singleValuesEnabledParameterWithConfiguration(@org.mule.sdk.api.annotation.values.OfValues(WithConfigValueProvider.class) String channels,
                                                            @Connection ValuesConnection connection) {}

  public void singleValuesEnabledParameterWithRequiredParameters(@OfValues(WithRequiredParametersValueProvider.class) String channels,
                                                                 String requiredString,
                                                                 boolean requiredBoolean,
                                                                 int requiredInteger,
                                                                 List<String> strings) {}

  public void singleValuesEnabledParameterInsideParameterGroup(@ParameterGroup(
      name = "ValuesGroup") GroupWithValuesParameter optionsParameter) {}

  public void singleValuesEnabledParameterRequiresValuesOfParameterGroup(@org.mule.sdk.api.annotation.values.OfValues(WithRequiredParameterFromGroupValueProvider.class) String values,
                                                                         @ParameterGroup(
                                                                             name = "ValuesGroup") GroupWithValuesParameter optionsParameter) {}

  public void multiLevelValue(@OfValues(MultiLevelValueProvider.class) @ParameterGroup(
      name = "values") GroupAsMultiLevelValue optionsParameter) {

  }

  public void singleValuesWithRequiredParameterWithAlias(@ParameterGroup(
      name = "someGroup") WithRequiredParameterWithAliasGroup group) {}

  public void resolverGetsMuleContextInjection(@OfValues(WithMuleContextValueProvider.class) String channel) {

  }

  public void valuesInsideShowInDslGroup(@org.mule.sdk.api.annotation.values.OfValues(WithRequiredParameterFromGroupValueProvider.class) String values,
                                         @ParameterGroup(name = "ValuesGroup",
                                             showInDsl = true) GroupWithValuesParameter optionsParameter) {

  }

  public void withErrorValueProvider(@org.mule.sdk.api.annotation.values.OfValues(WithErrorValueProvider.class) String values,
                                     String errorCode) {

  }

  public void withComplexActingParameter(@Optional @OfValues(WithComplexActingParameter.class) String providedParameter,
                                         ComplexActingParameter complexActingParameter) {}

  public void withRequiredParameter(@OfValues(WithRequiredParameterValueProvider.class) String providedParameters,
                                    String requiredValue) {}

  public void withRequiredParameterAndOptionalParameterAsRequired(@OfValues(WithRequiredAndOptionalParametersValueProvider.class) String providedParameters,
                                                                  String requiredValue, String optionalValue) {}

  public void withRequiredAndOptionalParameters(@OfValues(WithRequiredAndOptionalParametersValueProvider.class) String providedParameters,
                                                String requiredValue, @Optional String optionalValue) {}

  public void withRequiredAndOptionalWithDefaultParameters(@OfValues(WithRequiredAndOptionalParametersValueProvider.class) String providedParameters,
                                                           String requiredValue, @Optional(
                                                               defaultValue = "OPERATION_DEFAULT_VALUE") String optionalValue) {}

  public void withOptionalParameterAsRequired(@OfValues(WithOptionalParametersValueProvider.class) String providedParameters,
                                              String optionalValue) {}

  public void withOptionalParameter(@OfValues(WithOptionalParametersValueProvider.class) String providedParameters,
                                    @Optional String optionalValue) {}

  public void withOptionalParameterWithDefault(@OfValues(WithOptionalParametersValueProvider.class) String providedParameters,
                                               @Optional(defaultValue = "OPERATION_DEFAULT_VALUE") String optionalValue) {}

  public void withVPOptionalParameterWithDefaultValue(@OfValues(WithOptionalParametersWithDefaultValueProvider.class) String providedParameters,
                                                      @Optional(defaultValue = "OPERATION_DEFAULT_VALUE") String optionalValue) {}

  public void withBoundActingParameter(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {
          @Binding(actingParameter = "requiredValue", extractionExpression = "actingParameter")}) String parameterWithValues,
                                       String actingParameter) {}

  public void withBoundOptionalActingParameter(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithOptionalParameterSdkValueProvider.class,
      bindings = {
          @Binding(actingParameter = "optionalValue", extractionExpression = "actingParameter")}) String parameterWithValues,
                                               String actingParameter) {}

  public void withBoundActingParameterField(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue",
          extractionExpression = "actingParameter.field")}) String parameterWithValues,
                                            @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withBoundOptionalActingParameterField(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithOptionalParameterSdkValueProvider.class,
      bindings = {@Binding(actingParameter = "optionalValue",
          extractionExpression = "actingParameter.nested.field")}) String parameterWithValues,
                                                    @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withBoundActingParameterFieldWithDot(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {
          @Binding(actingParameter = "requiredValue",
              extractionExpression = "actingParameter.\"field.with.dot\"")}) String parameterWithValues,
                                                   @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withTwoActingParameters(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithTwoActingParametersValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue",
          extractionExpression = "actingParameter.field")}) String parameterWithValues,
                                      String scalarActingParameter,
                                      @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withTwoBoundActingParameters(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithTwoActingParametersValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue", extractionExpression = "actingParameter.field"),
          @Binding(actingParameter = "scalarActingParameter",
              extractionExpression = "anotherParameter")}) String parameterWithValues,
                                           String anotherParameter,
                                           @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}


  public void withBoundActingParameterToXmlTagContent(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {
          @Binding(actingParameter = "requiredValue",
              extractionExpression = "actingParameter.nested.xmlTag")}) String parameterWithValues,
                                                      @TypeResolver(XmlTypeResolver.class) InputStream actingParameter) {}

  public void withBoundActingParameterToXmlTagAttribute(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue",
          extractionExpression = "actingParameter.nested.xmlTag.@attribute")}) String parameterWithValues,
                                                        @TypeResolver(XmlTypeResolver.class) InputStream actingParameter) {}

  public void withFourBoundActingParameters(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithFourActingParametersValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue", extractionExpression = "actingParameter.field1"),
          @Binding(actingParameter = "anotherValue", extractionExpression = "actingParameter.nested.field2"),
          @Binding(actingParameter = "someValue", extractionExpression = "actingParameter.nested.field3"),
          @Binding(actingParameter = "optionalValue",
              extractionExpression = "actingParameter.anotherNested.field4")}) String parameterWithValues,
                                            @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withBoundActingParameterArray(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithListParameterValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue",
          extractionExpression = "actingParameter.jsonArray")}) String parameterWithValues,
                                            @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withPojoBoundActingParameter(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithPojoParameterValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue",
          extractionExpression = "actingParameter.pojoField")}) String parameterWithValues,
                                           @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withMapBoundActingParameter(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithMapParameterValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue",
          extractionExpression = "actingParameter.mapField")}) String parameterWithValues,
                                          @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withPojoFieldBoundActingParameterField(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue",
          extractionExpression = "actingParameter.pojoId")}) String parameterWithValues,
                                                     MyPojo actingParameter) {}

  public void withBoundActingParameterEnum(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithEnumParameterValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue",
          extractionExpression = "actingParameter.enumField")}) String parameterWithValues,
                                           @TypeResolver(JsonTypeResolver.class) InputStream actingParameter) {}

  public void withBoundActingParameterWithAlias(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {
          @Binding(actingParameter = "requiredValue", extractionExpression = "parameterAlias")}) String parameterWithValues,
                                                @Alias("parameterAlias") String actingParameter) {}

  public void withBoundActingParameterFromContentField(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {
          @Binding(actingParameter = "requiredValue", extractionExpression = "body.field")}) String parameterWithValues,
                                                       @TypeResolver(JsonTypeResolver.class) @Content InputStream body) {}

  public void withBoundActingParameterFromXmlContentField(@org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {
          @Binding(actingParameter = "requiredValue", extractionExpression = "xmlBody.field")}) String parameterWithValues,
                                                          @TypeResolver(XmlTypeResolver.class) @Content InputStream xmlBody) {}

  public void singleValuesEnabledParameterWithOneFieldValues(@Content @FieldValues(targetSelectors = "simple.path",
      value = SimpleValueProvider.class) InputStream body) {}

  public void singleValuesEnabledParameterWithMoreThanOneFieldValues(@Content @FieldValues(targetSelectors = "simple.path",
      value = SimpleValueProvider.class) @FieldValues(targetSelectors = "another.simple.path",
          value = TrueFalseValueProvider.class) InputStream body) {}

}
