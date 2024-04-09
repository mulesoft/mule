/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.module.boot.commons.internal.AbstractMuleContainerFactory.APP_COMMAND_LINE_OPTION;
import static org.mule.runtime.module.boot.commons.internal.AbstractMuleContainerFactory.INVALID_DEPLOY_APP_CONFIGURATION_ERROR;

import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;

import org.mule.runtime.module.boot.api.MuleContainer;
import org.mule.runtime.module.boot.commons.internal.AbstractMuleContainerFactory;
import org.mule.runtime.module.boot.commons.internal.DefaultMuleClassPathConfig;
import org.mule.tck.MuleTestUtils.TestCallback;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class MuleContainerFactoryTestCase {

  private static final String APP_NAME = "testApp";

  private String[] commandLineOptions = {};

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void setsMuleDeployApplicationsPropertyWhenAppOptionIsUsed() throws Exception {
    try {
      commandLineOptions = new String[] {"-" + APP_COMMAND_LINE_OPTION, APP_NAME};
      createMuleContainer();

      assertThat(getProperty(DEPLOYMENT_APPLICATION_PROPERTY), equalTo(APP_NAME));
    } finally {
      if (getProperty(DEPLOYMENT_APPLICATION_PROPERTY) != null) {
        clearProperty(DEPLOYMENT_APPLICATION_PROPERTY);
      }
    }
  }

  @Test
  public void failsToStartWhenMuleDeployApplicationsPropertyAndAppOptionAreUsed() throws Exception {
    testWithSystemProperty(DEPLOYMENT_APPLICATION_PROPERTY, APP_NAME, () -> {
      commandLineOptions = new String[] {"-" + APP_COMMAND_LINE_OPTION, APP_NAME};

      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage(INVALID_DEPLOY_APP_CONFIGURATION_ERROR);
      createMuleContainer();
    });
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
  public static void testWithSystemProperty(String propertyName, String propertyValue, TestCallback callback)
      throws Exception {
    assert propertyName != null && callback != null;
    String originalPropertyValue = null;
    try {
      if (propertyValue == null) {
        originalPropertyValue = System.clearProperty(propertyName);
      } else {
        originalPropertyValue = System.setProperty(propertyName, propertyValue);
      }
      callback.run();
    } finally {
      if (originalPropertyValue == null) {
        System.clearProperty(propertyName);
      } else {
        System.setProperty(propertyName, originalPropertyValue);
      }
    }
  }

  private MuleContainer createMuleContainer() throws Exception {
    return new AbstractMuleContainerFactory(null, null) {

      @Override
      protected DefaultMuleClassPathConfig createMuleClassPathConfig(File muleHome, File muleBase) {
        return null;
      }

      @Override
      MuleContainer createMuleContainer() throws IOException, Exception {
        return mock(MuleContainer.class);
      }
    }.create(commandLineOptions);
  }

}
