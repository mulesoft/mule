/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.validation;

import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class ExtensionValidationTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String getConfigFile() {
    return "validation/invalid-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectCause(new TypeSafeMatcher<Throwable>() {

      @Override
      protected boolean matchesSafely(Throwable exception) {
        return exception.getMessage().contains("'invalid-config' is invalid");
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("exception cause was expected to have the content ''invalid-config' is invalid'");
      }
    });
  }

  @Test
  public void invalidConfig() throws Exception {
    fail("Configuration should have been invalid");
  }
}
