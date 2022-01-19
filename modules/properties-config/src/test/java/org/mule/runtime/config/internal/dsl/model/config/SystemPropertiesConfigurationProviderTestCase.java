/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static org.mule.tck.MuleTestUtils.testWithSystemProperty;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class SystemPropertiesConfigurationProviderTestCase extends AbstractMuleTestCase {

  private SystemPropertiesConfigurationProvider systemPropertiesConfigurationProvider;

  @Test
  public void systemProperty() throws Exception {
    String propertyKey = "propertyA";
    String propertyValue = "propertyAValue";
    testWithSystemProperty(propertyKey, propertyValue, () -> {
      systemPropertiesConfigurationProvider = new SystemPropertiesConfigurationProvider();
      assertThat(systemPropertiesConfigurationProvider.provide(propertyKey).get().getValue(),
                 is(propertyValue));
    });
  }

  @Test
  public void systemPropertyReadAtInit() throws Exception {
    String propertyKey = "propertyA";
    String propertyValue = "propertyAValue";
    testWithSystemProperty(propertyKey, propertyValue, () -> {
      systemPropertiesConfigurationProvider = new SystemPropertiesConfigurationProvider();
    });

    assertThat(systemPropertiesConfigurationProvider.provide(propertyKey).get().getValue(),
               is(propertyValue));
  }

}
