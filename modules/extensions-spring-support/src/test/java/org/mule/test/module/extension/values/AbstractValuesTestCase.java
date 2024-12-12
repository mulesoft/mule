/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.tck.junit4.matcher.value.ValueResultSuccessMatcher.isSuccess;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.ValueProvidersStory.VALUE_PROVIDERS_SERVICE;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItems;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.tck.junit4.matcher.ValueMatcher;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.hamcrest.Matcher;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
@Story(VALUE_PROVIDERS_SERVICE)
@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.mule.tests:mule-tests-model"})
public abstract class AbstractValuesTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  @Named(VALUE_PROVIDER_SERVICE_KEY)
  private ValueProviderService valueProviderService;

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Override
  public boolean addToolingObjectsToRegistry() {
    return true;
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  Matcher<Iterable<Value>> hasValues(String... values) {
    Set<ValueMatcher> options = stream(values)
        .map(ValueMatcher::valueWithId)
        .collect(toSet());
    return hasValues(options.toArray(new ValueMatcher[] {}));
  }

  Matcher<Iterable<Value>> hasValues(ValueMatcher... valuesMatchers) {
    return hasItems(valuesMatchers);
  }

  Set<Value> getValuesFromSource(String flowName, String parameterName) throws Exception {
    ValueResult valueResult =
        valueProviderService.getValues(Location.builder().globalName(flowName).addSourcePart().build(), parameterName);

    assertThat(valueResult, isSuccess());
    return valueResult.getValues();
  }

  Set<Value> getValuesFromSource(String flowName, String parameterName, String targetSelector) throws Exception {
    ValueResult valueResult =
        valueProviderService.getFieldValues(Location.builder().globalName(flowName).addSourcePart().build(), parameterName,
                                            targetSelector);

    assertThat(valueResult, isSuccess());
    return valueResult.getValues();
  }

  Set<Value> getValues(String flowName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(getValueResult(flowName, parameterName));
  }

  ValueResult getValueResult(String flowName, String parameterName) throws Exception {
    Location location = Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build();
    return valueProviderService.getValues(location, parameterName);
  }

  Set<Value> getValues(String flowName, String parameterName, String targetSelector) throws Exception {
    return checkResultAndRetrieveValues(getValueResult(flowName, parameterName, targetSelector));
  }

  ValueResult getValueResult(String flowName, String parameterName, String targetSelector) throws Exception {
    Location location = Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build();
    return valueProviderService.getFieldValues(location, parameterName, targetSelector);
  }

  Set<Value> getValuesFromConfig(String configName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(getValueResultFromConfig(configName, parameterName));
  }

  public ValueResult getValueResultFromConfig(String configName, String parameterName) {
    return valueProviderService.getValues(Location.builder().globalName(configName).build(),
                                          parameterName);
  }

  Set<Value> getFieldValuesFromConfig(String configName, String parameterName, String targetSelector) throws Exception {
    return checkResultAndRetrieveValues(getFieldValuesResultFromConfig(configName, parameterName, targetSelector));
  }

  public ValueResult getFieldValuesResultFromConfig(String configName, String parameterName, String targetSelector) {
    return valueProviderService.getFieldValues(Location.builder().globalName(configName).build(),
                                               parameterName, targetSelector);
  }

  Set<Value> getValuesFromConnection(String configName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(getValueResultFromConnection(configName, parameterName));
  }

  public ValueResult getValueResultFromConnection(String configName, String parameterName) {
    return valueProviderService
        .getValues(Location.builder().globalName(configName).addConnectionPart().build(), parameterName);
  }

  Set<Value> getFieldValuesFromConnection(String configName, String parameterName, String targetSelector) throws Exception {
    return checkResultAndRetrieveValues(getFieldValueResultFromConnection(configName, parameterName, targetSelector));
  }

  public ValueResult getFieldValueResultFromConnection(String configName, String parameterName, String targetSelector) {
    return valueProviderService
        .getFieldValues(Location.builder().globalName(configName).addConnectionPart().build(), parameterName, targetSelector);
  }

  private Set<Value> checkResultAndRetrieveValues(ValueResult values) throws ValueResolvingException {
    assertThat(values, isSuccess());
    return values.getValues();
  }

}
