/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

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
