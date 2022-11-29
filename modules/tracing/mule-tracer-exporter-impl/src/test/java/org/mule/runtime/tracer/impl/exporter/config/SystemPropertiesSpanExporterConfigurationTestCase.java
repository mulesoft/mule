/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.config;

import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class SystemPropertiesSpanExporterConfigurationTestCase {

  @Test
  public void systemProperty() throws Exception {
    String propertyKey = "propertyA";
    String propertyValue = "propertyAValue";
    testWithSystemProperty(propertyKey, propertyValue, () -> {
      SystemPropertiesSpanExporterConfiguration configuration = new SystemPropertiesSpanExporterConfiguration();
      assertThat(configuration.getValue(propertyKey),
                 is(propertyValue));

      assertThat(configuration.getValue("propertyNonExizting"), is(nullValue()));
    });
  }
}
