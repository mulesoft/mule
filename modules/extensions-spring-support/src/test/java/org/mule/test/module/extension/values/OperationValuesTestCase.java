/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mule.runtime.extension.api.values.ValueResolvingException.MISSING_REQUIRED_PARAMETERS;
import static org.mule.tck.junit4.matcher.ValueMatcher.valueWithId;
import static org.mule.test.values.extension.resolver.WithErrorValueProvider.ERROR_MESSAGE;

import org.mule.runtime.api.value.ResolvingFailure;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;
import org.mule.tck.junit4.matcher.ValueMatcher;

import java.util.Set;

import org.junit.Test;

public class OperationValuesTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/operation-values.xml";
  }

  @Test
  public void singleOptions() throws Exception {
    Set<Value> channels = getValues("single-values-enabled-parameter", "channels");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void singleOptionsEnabledParameterWithConnection() throws Exception {
    Set<Value> channels = getValues("singleValuesEnabledParameterWithConnection", "channels");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("connection1", "connection2", "connection3"));
  }

  @Test
  public void singleOptionsEnabledParameterWithConfiguration() throws Exception {
    Set<Value> channels = getValues("singleValuesEnabledParameterWithConfiguration", "channels");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("config1", "config2", "config3"));
  }

  @Test
  public void singleOptionsEnabledParameterWithRequiredParameters() throws Exception {
    Set<Value> channels = getValues("singleValuesEnabledParameterWithRequiredParameters", "channels");
    assertThat(channels, hasSize(4));
    assertThat(channels, hasValues("requiredInteger:2", "requiredBoolean:false", "strings:[1, 2]", "requiredString:aString"));
  }

  @Test
  public void singleOptionsEnabledParameterWithRequiredParametersUsingExpressions() throws Exception {
    Set<Value> channels = getValues("singleOptionsEnabledParameterWithRequiredParametersUsingExpressions", "channels");
    assertThat(channels, hasSize(4));
    assertThat(channels, hasValues("requiredInteger:2", "requiredBoolean:false", "strings:[1, 2]", "requiredString:aString"));
  }

  @Test
  public void singleOptionsEnabledParameterWithMissingRequiredParameters() throws Exception {
    ValueResult valueResult = getValueResult("singleOptionsEnabledParameterWithMissingRequiredParameters", "channels");
    assertThat(valueResult.isSuccess(), is(false));
    assertThat(valueResult.getFailure().get().getFailureCode(), is(MISSING_REQUIRED_PARAMETERS));
  }

  @Test
  public void singleOptionsEnabledParameterWithOptionalParameter() throws Exception {
    Set<Value> channels = getValues("singleOptionsEnabledParameterWithOptionalParameter", "channels");
    assertThat(channels, hasSize(4));
    assertThat(channels, hasValues("requiredInteger:2", "requiredBoolean:false", "strings:[1, 2]", "requiredString:null"));
  }

  @Test
  public void singleOptionsEnabledParameterInsideParameterGroup() throws Exception {
    Set<Value> channels = getValues("singleValuesEnabledParameterInsideParameterGroup", "channels");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void singleOptionsEnabledParameterRequiresValuesOfParameterGroup() throws Exception {
    Set<Value> channels = getValues("singleValuesEnabledParameterRequiresValuesOfParameterGroup", "values");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:aParam"));
  }

  @Test
  public void multiLevelOption() throws Exception {
    Set<Value> values = getValues("multiLevelValue", "values");
    ValueMatcher americaValue = valueWithId("America")
        .withDisplayName("America")
        .withPartName("continent")
        .withChilds(valueWithId("Argentina")
            .withDisplayName("Argentina")
            .withPartName("country")
            .withChilds(valueWithId("Buenos Aires")
                .withDisplayName("Buenos Aires")
                .withPartName("city")));

    assertThat(values, hasValues(americaValue));
  }

  @Test
  public void singleOptionsWithRequiredParameterWithAlias() throws Exception {
    Set<Value> channels = getValues("singleValuesWithRequiredParameterWithAlias", "channels");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("requiredString:dummyValue"));
  }

  @Test
  public void resolverGetsMuleContextInjection() throws Exception {
    Set<Value> values = getValues("resolverGetsMuleContextInjection", "channel");
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("INJECTED!!!"));
  }

  @Test
  public void optionsInsideShowInDslGroup() throws Exception {
    Set<Value> values = getValues("valuesInsideShowInDslGroup", "values");
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("anyParameter:someValue"));
  }

  @Test
  public void optionsInsideShowInDslDynamicGroup() throws Exception {
    Set<Value> values = getValues("valuesInsideShowInDslDynamicGroup", "values");
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("anyParameter:someValue"));
  }

  @Test
  public void userErrorWhenResolvingValues() throws Exception {
    ValueResult result = getValueResult("withErrorValueProvider", "values");
    assertThat(result.getFailure().isPresent(), is(true));
    ResolvingFailure resolvingFailure = result.getFailure().get();
    assertThat(resolvingFailure.getFailureCode(), is("CUSTOM_ERROR"));
    assertThat(resolvingFailure.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void withBoundActingParameter() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameter", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    // Add assertion
  }

  @Test
  public void withBoundActingParameterField() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterField", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    // Add assertion
  }
  //
  //  public void withTwoActingParameters(@OfValues(
  //      value = WithTwoActingParametersValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue", path = "actingParameter.field")}) String parameterWithValues,
  //                                      String scalarActingParameter,
  //                                      InputStream actingParameter) {}
  //
  //  public void withTwoBoundActingParameters(@OfValues(
  //      value = WithTwoActingParametersValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue", path = "actingParameter.field"),
  //          @Binding(actingParameter = "scalarActingParameter", path = "anotherParameter")}) String parameterWithValues,
  //                                           String anotherParameter,
  //                                           InputStream actingParameter) {}
  //
  //
  //  public void withBoundActingParameterToXmlTagContent(@OfValues(
  //      value = WithRequiredParameterValueProvider.class,
  //      bindings = {
  //          @Binding(actingParameter = "requiredValue", path = "actingParameter.nested.xmlTag")}) String parameterWithValues,
  //                                                      InputStream actingParameter) {}
  //
  //  public void withBoundActingParameterToXmlTagAtttribute(@OfValues(
  //      value = WithRequiredParameterValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue",
  //          path = "actingParameter.nested.xmlTag.@attribute")}) String parameterWithValues,
  //                                                         InputStream actingParameter) {}
  //
  //  public void withFourBoundActingParametes(@OfValues(
  //      value = WithFourActingParametersValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue", path = "actingParameter.field1"),
  //          @Binding(actingParameter = "anotherValue", path = "actingParameter.nested.field2"),
  //          @Binding(actingParameter = "someValue", path = "actingParameter.nested.field.3"),
  //          @Binding(actingParameter = "optionalValue", path = "actingParameter.anotherNested.field4")}) String parameterWithValues,
  //                                           InputStream actingParameter) {}
  //
  //  public void withBoundActingParameterArray(@OfValues(
  //      value = WithArrayParameterValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue", path = "actingParameter.jsonArray")}) String parameterWithValues,
  //                                            InputStream actingParameter) {}
  //
  //  public void withPojoBoundActingParameter(@OfValues(
  //      value = WithPojoParameterValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue", path = "actingParameter.pojoField")}) String parameterWithValues,
  //                                           InputStream actingParameter) {}
  //
  //  public void withMapBoundActingParameter(@OfValues(
  //      value = WithMapParameterValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue", path = "actingParameter.mapField")}) String parameterWithValues,
  //                                          InputStream actingParameter) {}
  //
  //  // Test both defining pojo as an expression and in the dsl.
  //  public void withPojoFieldBoundActingParameterField(@OfValues(
  //      value = WithRequiredParameterValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue", path = "actingParameter.pojoId")}) String parameterWithValues,
  //                                                     MyPojo actingParameter) {}
  //
  //  public void withBoundActingParameterEnum(@OfValues(
  //      value = WithEnumParameterValueProvider.class,
  //      bindings = {@Binding(actingParameter = "requiredValue", path = "actingParameter.enumField")}) String parameterWithValues,
  //                                           InputStream actingParameter) {}
}
