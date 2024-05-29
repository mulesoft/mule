/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.internal;

import static org.mule.runtime.core.util.internal.StandaloneServerUtils.MULE_BASE_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.util.internal.StandaloneServerUtils.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.util.internal.StandaloneServerUtils.getMuleBase;
import static org.mule.runtime.core.util.internal.StandaloneServerUtils.getMuleHome;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class StandaloneServerUtilsTestCase {

  private static final String EXPECTED_MULE_HOME_VALUE = "expected-mule-home-value";
  private static final String EXPECTED_MULE_BASE_VALUE = "expected-mule-base-value";

  @Test
  public void muleHome() throws Exception {
    testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, EXPECTED_MULE_HOME_VALUE,
                           () -> assertThat(getMuleHome().get().getName(), is(EXPECTED_MULE_HOME_VALUE)));
  }

  @Test
  public void muleHomeIsNullWhenNotDefined() throws Exception {
    assertThat(getMuleHome().isPresent(), is(false));
  }

  @Test
  public void muleBase() throws Exception {
    testWithSystemProperty(MULE_BASE_DIRECTORY_PROPERTY, EXPECTED_MULE_BASE_VALUE,
                           () -> assertThat(getMuleBase().get().getName(), is(EXPECTED_MULE_BASE_VALUE)));
  }

  @Test
  public void muleBaseReturnsMuleHomeWhenNotSet() throws Exception {
    testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, EXPECTED_MULE_HOME_VALUE,
                           () -> assertThat(getMuleBase().get().getName(), is(EXPECTED_MULE_HOME_VALUE)));
  }

  @Test
  public void muleBaseReturnsNullIfNetherMuleHomeOrMuleBaseIsSet() throws Exception {
    assertThat(getMuleBase().isPresent(), is(false));
  }


  /**
   * Executes callback with a given system property set and replaces the system property with it's original value once done.
   * Useful for asserting behaviour that is dependent on the presence of a system property.
   *
   * @param propertyName  Name of system property to set
   * @param propertyValue Value of system property
   * @param callback      Callback implementing the the test code and assertions to be run with system property set.
   * @throws Exception any exception thrown by the execution of callback
   */
  public static void testWithSystemProperty(String propertyName, Object propertyValue, Runnable callback)
      throws Exception {
    assert propertyName != null && callback != null;
    Object originalPropertyValue = null;
    try {
      if (propertyValue == null) {
        originalPropertyValue = System.getProperties().remove(propertyName);
      } else {
        originalPropertyValue = System.getProperties().put(propertyName, propertyValue);
      }
      callback.run();
    } finally {
      if (originalPropertyValue == null) {
        System.getProperties().remove(propertyName);
      } else {
        System.getProperties().put(propertyName, originalPropertyValue);
      }
    }
  }
}
