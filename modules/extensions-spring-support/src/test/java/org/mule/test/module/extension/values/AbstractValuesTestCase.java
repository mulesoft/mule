/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.value.ResolvingFailure;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.tck.junit4.matcher.ValueMatcher;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Set;

import org.hamcrest.Matcher;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
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
    if (valueResult.getFailure().isPresent()) {
      ResolvingFailure resolvingFailure = valueResult.getFailure().get();
      throw new ValueResolvingException(resolvingFailure.getMessage(), resolvingFailure.getFailureCode());
    }
    return valueResult
        .getValues();
  }

  Set<Value> getValues(String flowName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(getValueResult(flowName, parameterName));
  }

  ValueResult getValueResult(String flowName, String parameterName) throws Exception {
    Location location = Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build();
    return valueProviderService.getValues(location, parameterName);
  }

  Set<Value> getValuesFromConfig(String configName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(valueProviderService.getValues(Location.builder().globalName(configName).build(),
                                                                       parameterName));
  }

  Set<Value> getValuesFromConnection(String configName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(valueProviderService
        .getValues(Location.builder().globalName(configName).addConnectionPart().build(), parameterName));
  }

  private Set<Value> checkResultAndRetrieveValues(ValueResult values) throws ValueResolvingException {
    if (!values.isSuccess()) {
      ResolvingFailure resolvingFailure = values.getFailure().get();
      throw new ValueResolvingException(resolvingFailure.getMessage(), resolvingFailure.getFailureCode());
    }
    return values.getValues();
  }
}
