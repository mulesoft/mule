/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import static org.mule.test.values.extension.resolver.MultiLevelValueProvider.AMERICA;
import static org.mule.test.values.extension.resolver.MultiLevelValueProvider.ARGENTINA;
import static org.mule.test.values.extension.resolver.MultiLevelValueProvider.BUENOS_AIRES;
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
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withBoundActingParameterWithAlias() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterWithAlias", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withBoundActingParameterField() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterField", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withBoundActingParameterFieldWithDot() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterFieldWithDot", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withTwoActingParameters() throws Exception {
    ValueResult result = getValueResult("withTwoActingParameters", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(2));
    assertThat(values, hasValues("Acting parameter value", "Scalar value"));
  }

  @Test
  public void withTwoBoundActingParameters() throws Exception {
    ValueResult result = getValueResult("withTwoBoundActingParameters", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(2));
    assertThat(values, hasValues("Acting parameter value", "Scalar value"));
  }

  @Test
  public void withBoundActingParameterToXmlTagContent() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterToXmlTagContent", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("This is the tag content"));
  }

  @Test
  public void withBoundActingParameterToXmlTagAttribute() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterToXmlTagAttribute", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("This is the attribute value"));
  }

  @Test
  public void withFourBoundActingParameters() throws Exception {
    ValueResult result = getValueResult("withFourBoundActingParameters", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(4));
    assertThat(values, hasValues("Field1 Value", "Field2 Value", "Field3 Value", "Field4 Value"));
  }

  @Test
  public void withBoundActingParameterArray() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterArray", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(3));
    assertThat(values, hasValues("One Value", "Another value", "Yet another value"));
  }

  @Test
  public void withPojoBoundActingParameter() throws Exception {
    ValueResult result = getValueResult("withPojoBoundActingParameter", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values,
               hasValues("MyPojo{pojoId='This is the pojo ID', pojoName='This is the pojo name', pojoNumber=23, pojoBoolean=true}"));
  }

  @Test
  public void withMapBoundActingParameter() throws Exception {
    ValueResult result = getValueResult("withMapBoundActingParameter", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(4));
    assertThat(values, hasValues("pojoId : This is the pojo ID", "pojoName : This is the pojo name", "pojoNumber : 23",
                                 "pojoBoolean : true"));
  }

  @Test
  public void withMissingBoundActingParameter() throws Exception {
    ValueResult result = getValueResult("withMissingBoundActingParameter", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(true));
    assertThat(result.getFailure().get().getMessage(),
               is("Unable to retrieve values. There are missing required parameters for the resolution: [requiredValue(taken from: actingParameter.field)]"));
  }

  @Test
  public void withPojoFieldBoundActingParameterFieldExpression() throws Exception {
    ValueResult result = getValueResult("withPojoFieldBoundActingParameterFieldExpression", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("This is the pojo ID"));
  }

  @Test
  public void withPojoFieldBoundActingParameterFieldDsl() throws Exception {
    ValueResult result = getValueResult("withPojoFieldBoundActingParameterFieldDsl", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("This is the pojo ID"));
  }

  @Test
  public void withPojoFieldBoundIncompleteActingParameterFieldDsl() throws Exception {
    ValueResult result = getValueResult("withPojoFieldBoundIncompleteActingParameterFieldDsl", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("This is the pojo ID"));
  }

  @Test
  public void withBoundActingParameterEnum() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterEnum", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("ENUM_VALUE"));
  }

  @Test
  public void withBoundOptionalActingParameterPresent() throws Exception {
    ValueResult result = getValueResult("withBoundOptionalActingParameterPresent", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withBoundOptionalActingParameter() throws Exception {
    ValueResult result = getValueResult("withBoundOptionalActingParameter", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Optional value ommited"));
  }

  @Test
  public void withBoundOptionalActingParameterFieldPresent() throws Exception {
    ValueResult result = getValueResult("withBoundOptionalActingParameterFieldPresent", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withBoundOptionalActingParameterFieldMissingParameter() throws Exception {
    ValueResult result = getValueResult("withBoundOptionalActingParameterFieldMissingParameter", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Optional value ommited"));
  }

  @Test
  public void withBoundOptionalActingParameterFieldMissingField() throws Exception {
    ValueResult result = getValueResult("withBoundOptionalActingParameterFieldMissingField", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Optional value ommited"));
  }

  @Test
  public void withBoundOptionalActingParameterFieldMissingPath() throws Exception {
    ValueResult result = getValueResult("withBoundOptionalActingParameterFieldMissingPath", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Optional value ommited"));
  }

  @Test
  public void withBoundActingParameterFromContentField() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterFromContentField", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withBoundActingParameterFromContentFieldWithExpression() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterFromContentFieldWithExpression", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withBoundActingParameterFromXmlContentField() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterFromXmlContentField", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void withBoundActingParameterFromXmlContentFieldWithExpression() throws Exception {
    ValueResult result = getValueResult("withBoundActingParameterFromXmlContentFieldWithExpression", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void singleValuesEnabledParameterWithMoreThanOneFieldValues() throws Exception {
    ValueResult result1 = getValueResult("singleValuesEnabledParameterWithMoreThanOneFieldValues", "body", "simple.path");
    assertThat(result1.getValues(), hasSize(3));
    assertThat(result1.getValues(), hasValues("channel1", "channel2", "channel3"));

    ValueResult result2 = getValueResult("singleValuesEnabledParameterWithMoreThanOneFieldValues", "body", "another.simple.path");
    assertThat(result2.getValues(), hasSize(1));
    assertThat(result2.getValues(), hasValues("FALSE"));
  }

  @Test
  public void singleValuesEnabledParameterWithOneFieldValues() throws Exception {
    ValueResult result = getValueResult("singleValuesEnabledParameterWithOneFieldValues", "body", "simple.path");
    assertThat(result.getValues(), hasSize(3));
    assertThat(result.getValues(), hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void parameterWithTwoFieldWithSameValues() throws Exception {
    ValueResult targetResult = getValueResult("parameterWithTwoFieldWithSameValues", "body", "target.channelId");
    ValueResult sourceResult = getValueResult("parameterWithTwoFieldWithSameValues", "body", "source.channelId");

    String targetPartName = "body.target.channelId";
    assertThat(targetResult.getValues(), hasSize(3));
    assertThat(targetResult.getValues(), hasValues(valueWithId("channel1")
        .withPartName(targetPartName), valueWithId("channel2")
            .withPartName(targetPartName), valueWithId("channel3")
                .withPartName(targetPartName)));


    String sourcePartName = "body.source.channelId";
    assertThat(sourceResult.getValues(), hasSize(3));
    assertThat(sourceResult.getValues(), hasValues(valueWithId("channel1")
        .withPartName(sourcePartName), valueWithId("channel2")
            .withPartName(sourcePartName), valueWithId("channel3")
                .withPartName(sourcePartName)));
  }

  @Test
  public void parameterWithMultiLevelFieldValues() throws Exception {
    Set<Value> values = getValues("parameterWithMultilevelFieldValue", "body", "location.continent");

    ValueMatcher americaValue = valueWithId(AMERICA)
        .withDisplayName(AMERICA)
        .withPartName("body.location.continent")
        .withChilds(valueWithId(ARGENTINA)
            .withDisplayName(ARGENTINA)
            .withPartName("body.location.country")
            .withChilds(valueWithId(BUENOS_AIRES)
                .withDisplayName(BUENOS_AIRES)
                .withPartName("body.location.city")));

    assertThat(values, hasValues(americaValue));
  }

  @Test
  public void parameterWithMultipleMultiLevelFieldValuesWithSameVP() throws Exception {
    Set<Value> values = getValues("parameterWithMultipleMultiLevelFieldValuesWithSameVP", "body", "target.location.continent");

    ValueMatcher americaValue = valueWithId(AMERICA)
        .withDisplayName(AMERICA)
        .withPartName("body.target.location.continent")
        .withChilds(valueWithId(ARGENTINA)
            .withDisplayName(ARGENTINA)
            .withPartName("body.target.location.country")
            .withChilds(valueWithId(BUENOS_AIRES)
                .withDisplayName(BUENOS_AIRES)
                .withPartName("body.target.location.city")));

    assertThat(values, hasValues(americaValue));

    Set<Value> otherValues =
        getValues("parameterWithMultipleMultiLevelFieldValuesWithSameVP", "body", "source.location.continent");

    ValueMatcher otherAmericaValue = valueWithId(AMERICA)
        .withDisplayName(AMERICA)
        .withPartName("body.source.location.continent")
        .withChilds(valueWithId(ARGENTINA)
            .withDisplayName(ARGENTINA)
            .withPartName("body.source.location.country")
            .withChilds(valueWithId(BUENOS_AIRES)
                .withDisplayName(BUENOS_AIRES)
                .withPartName("body.source.location.city")));

    assertThat(otherValues, hasValues(otherAmericaValue));
  }

  @Test
  public void actingParameterWithReservedName() throws Exception {
    Set<Value> channels = getValues("actingParameterWithReservedName", "parameterWithValues");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("typeValue"));
  }

}
