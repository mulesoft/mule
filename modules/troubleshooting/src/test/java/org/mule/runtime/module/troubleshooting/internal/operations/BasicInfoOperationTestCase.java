/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static java.util.Collections.emptyMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BasicInfoOperationTestCase {

  private BasicInfoOperation basicInfoOperation;

  @Before
  public void setup() {
    basicInfoOperation = new BasicInfoOperation();
  }

  @After
  public void tearDown() {
    System.clearProperty("mule.sysprop");
    System.clearProperty("nonmule.sysprop");
  }

  @Test
  public void muleSystemProperties() throws IOException {
    System.setProperty("mule.sysprop", "someValue");

    final var writer = new StringWriter();
    basicInfoOperation.getCallback().execute(emptyMap(), writer);
    String result = writer.toString();

    assertThat(result, containsString("mule.sysprop: someValue"));
  }

  @Test
  public void nonMuleSystemProperties() throws IOException {
    System.setProperty("nonmule.sysprop", "someValue");

    final var writer = new StringWriter();
    basicInfoOperation.getCallback().execute(emptyMap(), writer);
    String result = writer.toString();

    assertThat(result, not(containsString("nonmule")));
  }
}
