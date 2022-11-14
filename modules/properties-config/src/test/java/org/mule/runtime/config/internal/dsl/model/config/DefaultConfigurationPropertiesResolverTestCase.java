/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;

import org.junit.Test;

public class DefaultConfigurationPropertiesResolverTestCase {

  private static final String NOT_FOUND_PROPERTY = "${not-found.prop}";

  @Test
  public void propertyResolverMustNotFailWithPropertyNotFound() {
    DefaultConfigurationPropertiesResolver resolver =
        new DefaultConfigurationPropertiesResolver(empty(), mock(ConfigurationPropertiesProvider.class), false);
    String value = resolver.apply(NOT_FOUND_PROPERTY);
    assertThat(value, is(NOT_FOUND_PROPERTY));
  }

  @Test(expected = PropertyNotFoundException.class)
  public void propertyResolverMustFailWithPropertyNotFound() {
    DefaultConfigurationPropertiesResolver resolver =
        new DefaultConfigurationPropertiesResolver(empty(), mock(ConfigurationPropertiesProvider.class), true);
    resolver.apply(NOT_FOUND_PROPERTY);
  }
}
