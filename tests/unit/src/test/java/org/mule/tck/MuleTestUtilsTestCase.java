/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.tck.MuleTestUtils.testWithSystemProperties;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

public class MuleTestUtilsTestCase {

  @Test
  public void withSystemPropertiesClearsUndefinedProperties() throws Exception {
    final String propertyKey = "theKey";
    final String propertyValue = "theValue";
    assertThat(System.getProperty(propertyKey), is(nullValue()));
    testWithSystemProperties(ImmutableMap.of(propertyKey, propertyValue), () -> {
    });
    assertThat(System.getProperty(propertyKey), is(nullValue()));
  }

  @Test
  public void withSystemPropertiesResetsDefinedProperties() throws Exception {
    final String propertyKey = "theKey";
    final String propertyValue = "theValue";
    final String propertyNewValue = "anotherValue";
    System.setProperty(propertyKey, propertyValue);
    assertThat(System.getProperty(propertyKey), is(propertyValue));
    testWithSystemProperties(ImmutableMap.of(propertyKey, propertyNewValue), () -> {
    });
    assertThat(System.getProperty(propertyKey), is(propertyValue));
  }

}
