/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

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

public class OperationSampleDataTestCase extends AbstractSampleDataTestCase {

  private static final String EXPECTED_PAYLOAD = "my payload";
  private static final String EXPECTED_ATTRIBUTES = "my attributes";
  private static final String CONF_PREFIX = "from-conf-";
  private static final String NULL_VALUE = "<<null>>";

  @Override
  protected String getConfigFile() {
    return "data/sample/operation-values.xml";
  }

  @Test
  public void connectionLess() throws Exception {
    assertMessage(getSample("connectionLess"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConnection() throws Exception {
    assertMessage(getSample("useConnection"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void nonBlocking() throws Exception {
    assertMessage(getSample("nonBlocking"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConfig() throws Exception {
    assertMessage(getSample("useConfig"), CONF_PREFIX + EXPECTED_PAYLOAD, CONF_PREFIX + EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroup() throws Exception {
    assertMessage(getSample("parameterGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroupWithOptional() throws Exception {
    assertMessage(getSample("parameterGroupWithOptional"), EXPECTED_PAYLOAD, NULL_VALUE);
  }

  @Test
  public void showInDslParameterGroup() throws Exception {
    assertMessage(getSample("showInDslParameterGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
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
}
