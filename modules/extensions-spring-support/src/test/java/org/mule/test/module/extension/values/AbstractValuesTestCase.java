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
import org.mule.runtime.api.values.Value;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.module.extension.internal.runtime.ExtensionComponent;
import org.mule.runtime.module.extension.internal.runtime.config.ConfigurationProviderToolingAdapter;
import org.mule.tck.junit4.matcher.ValueMatcher;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.hamcrest.Matcher;

import java.util.Set;

@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.mule.tests:mule-tests-unit"})
public abstract class AbstractValuesTestCase extends MuleArtifactFunctionalTestCase {

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

  private ExtensionComponent<?> getComponentFromFlow(String flowName) throws Exception {
    Flow flow = (Flow) getFlowConstruct(flowName);
    return (ExtensionComponent) flow.getProcessors().get(0);
  }

  Set<Value> getValues(String flowName, String parameterName) throws Exception {
    return getComponentFromFlow(flowName).getValues(parameterName);
  }

  Set<Value> getValuesFromConfig(String configName, String parameterName) throws Exception {
    ConfigurationProviderToolingAdapter config = muleContext.getRegistry().get(configName);
    return config.getConfigValues(parameterName);
  }

  Set<Value> getValuesFromConnection(String configName, String parameterName) throws Exception {
    ConfigurationProviderToolingAdapter config = muleContext.getRegistry().get(configName);
    return config.getConnectionValues(parameterName);
  }
}
