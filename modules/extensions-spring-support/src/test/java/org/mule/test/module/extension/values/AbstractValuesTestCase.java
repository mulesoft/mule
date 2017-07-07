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
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.values.Value;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.tck.junit4.matcher.ValueMatcher;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.hamcrest.Matcher;
import org.junit.Before;

import java.util.Set;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
public abstract class AbstractValuesTestCase extends MuleArtifactFunctionalTestCase {

  private MuleValueProviderService valueProviderService;

  @Before
  public void setUp() throws RegistrationException {
    valueProviderService = muleContext.getRegistry().lookupObject(MuleValueProviderService.class);
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
    return valueProviderService.getComponentValues(Location.builder().globalName(flowName).addSourcePart().build(), parameterName)
        .getValues();
  }

  Set<Value> getValues(String flowName, String parameterName) throws Exception {
    Location location = Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build();
    return valueProviderService.getComponentValues(location, parameterName)
        .getValues();
  }

  Set<Value> getValuesFromConfig(String configName, String parameterName) throws Exception {
    return valueProviderService.getConfigurationValues(Location.builder().globalName(configName).build(), parameterName)
        .getValues();
  }

  Set<Value> getValuesFromConnection(String configName, String parameterName) throws Exception {
    return valueProviderService.getConnectionProviderValues(Location.builder().globalName(configName).build(), parameterName)
        .getValues();
  }
}
