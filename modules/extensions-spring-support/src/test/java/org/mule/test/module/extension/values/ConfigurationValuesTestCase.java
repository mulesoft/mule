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
import static org.mule.test.values.extension.resolver.WithErrorValueProvider.ERROR_MESSAGE;

import org.mule.runtime.api.value.ResolvingFailure;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;

import java.util.Set;

import org.junit.Test;

public class ConfigurationValuesTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/configuration-values.xml";
  }

  @Test
  public void configWithValues() throws Exception {
    Set<Value> channels = getValuesFromConfig("config-with-value", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void configWithValuesWithRequiredParameters() throws Exception {
    Set<Value> channels = getValuesFromConfig("value-with-required-param", "channel");
    assertThat(channels, hasSize(2));
    assertThat(channels, hasValues("required2:value2", "required1:value1"));
  }

  @Test
  public void configWithValuesWithRequiredParamsFromParamGroup() throws Exception {
    Set<Value> channels = getValuesFromConfig("values-with-required-params-from-param-group-config", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void configWithValuesWithRequiredParamsFromShowInDslGroup() throws Exception {
    Set<Value> channels = getValuesFromConfig("values-with-required-params-from-show-in-dsl-group", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void dynamicConfigWithValuesWithRequiredParamsFromShowInDslStaticGroup() throws Exception {
    Set<Value> channels =
        getValuesFromConfig("dynamic-config-values-with-required-params-from-show-in-dsl-static-group", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void dynamicConfigWithValuesWithRequiredParamsFromShowInDslDynamicGroup() throws Exception {
    Set<Value> channels =
        getValuesFromConfig("dynamic-config-values-with-required-params-from-show-in-dsl-dynamic-group", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void dynamicConfigWithValues() throws Exception {
    Set<Value> channels = getValuesFromConfig("dynamic-config", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void userErrorWhenResolvingValues() throws Exception {
    ValueResult result = getValueResultFromConfig("failure-config", "values");
    assertThat(result.getFailure().isPresent(), is(true));
    ResolvingFailure resolvingFailure = result.getFailure().get();
    assertThat(resolvingFailure.getFailureCode(), is("CUSTOM_ERROR"));
    assertThat(resolvingFailure.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void userErrorWhenResolvingValuesDynamic() throws Exception {
    ValueResult result = getValueResultFromConfig("dynamic-failure-config", "values");
    assertThat(result.getFailure().isPresent(), is(true));
    ResolvingFailure resolvingFailure = result.getFailure().get();
    assertThat(resolvingFailure.getFailureCode(), is("CUSTOM_ERROR"));
    assertThat(resolvingFailure.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void withBoundActingParameter() throws Exception {
    ValueResult result = getValueResultFromConfig("with-bound-acting-parameter", "parameterWithValues");
    assertThat(result.getFailure().isPresent(), is(false));
    Set<Value> values = result.getValues();
    assertThat(values, hasSize(1));
    assertThat(values, hasValues("Acting parameter value"));
  }

  @Test
  public void configWithParameterWithFieldValues() throws Exception {
    Set<Value> channels =
        getFieldValuesFromConfig("config-with-parameter-with-field-values", "securityHeaders", "security.algorithm");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }
}
