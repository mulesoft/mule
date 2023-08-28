/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model.dsl.config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

public class EnvironmentPropertiesConfigurationProviderTestCase extends AbstractMuleTestCase {

  @Test
  public void environmentVariables() {
    String variableKey = "varA";
    String variableValue = "varAValue";
    EnvironmentPropertiesConfigurationProvider environmentPropertiesConfigurationProvider =
        new EnvironmentPropertiesConfigurationProvider(() -> ImmutableMap.<String, String>builder()
            .put(variableKey, variableValue).build());
    assertThat(environmentPropertiesConfigurationProvider.provide(variableKey).get().getValue(),
               is(variableValue));
  }

}
