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

public class ConnectionProviderValuesTestCase extends AbstractValuesTestCase {

  @Override
  protected String getConfigFile() {
    return "values/connection-provider-values.xml";
  }

  @Test
  public void connectionWithValues() throws Exception {
    Set<Value> channels = getValuesFromConnection("with-value-parameter-connection", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void connectionWithValuesAndRequiredParameters() throws Exception {
    Set<Value> channels = getValuesFromConnection("with-value-with-required-param-connection", "channel");
    assertThat(channels, hasSize(2));
    assertThat(channels, hasValues("required2:value2", "required1:value1"));
  }

  @Test
  public void connectionWithValuesWithRequiredParamsFromParamGroup() throws Exception {
    Set<Value> channels = getValuesFromConnection("values-with-required-params-from-param-group-connection", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void connectionWithValuesWithRequiredParamsFromShowInDslGroup() throws Exception {
    Set<Value> channels =
        getValuesFromConnection("values-with-required-params-from-show-in-dsl-group-connection", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void dynamicConnectionWithValuesWithParamsFromShowInDslGroup() throws Exception {
    Set<Value> channels =
        getValuesFromConnection("values-with-required-params-from-show-in-dsl-group-dynamic-connection", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void dynamicConnectionWithValuesWithParamsFromShowInDslStaticGroup() throws Exception {
    Set<Value> channels =
        getValuesFromConnection("values-with-required-params-from-show-in-dsl-static-group-dynamic-connection", "valueParam");
    assertThat(channels, hasSize(1));
    assertThat(channels, hasValues("anyParameter:someValue"));
  }

  @Test
  public void dynamicConnectionWithValues() throws Exception {
    Set<Value> channels = getValuesFromConnection("dynamic-connection", "channel");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

  @Test
  public void userErrorWhenResolvingValues() throws Exception {
    ValueResult result = getValueResultFromConnection("failure-config", "values");
    assertThat(result.getFailure().isPresent(), is(true));
    ResolvingFailure resolvingFailure = result.getFailure().get();
    assertThat(resolvingFailure.getFailureCode(), is("CUSTOM_ERROR"));
    assertThat(resolvingFailure.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void userErrorWhenResolvingValuesDynamic() throws Exception {
    ValueResult result = getValueResultFromConnection("dynamic-failure-config", "values");
    assertThat(result.getFailure().isPresent(), is(true));
    ResolvingFailure resolvingFailure = result.getFailure().get();
    assertThat(resolvingFailure.getFailureCode(), is("CUSTOM_ERROR"));
    assertThat(resolvingFailure.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void withValueFourBoundActingParameters() throws Exception {
    Set<Value> channels = getValuesFromConnection("with-value-four-bound-acting-parameters", "parameterWithValue");
    assertThat(channels, hasSize(4));
    assertThat(channels, hasValues("some value", "another value", "a value", "last value"));
  }

  @Test
  public void connectionWithParameterWithFieldValues() throws Exception {
    Set<Value> channels =
        getFieldValuesFromConnection("with-parameter-with-field-values-connection", "urlFormat", "url.protocol");
    assertThat(channels, hasSize(3));
    assertThat(channels, hasValues("channel1", "channel2", "channel3"));
  }

}
