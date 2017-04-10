/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class ExpressionSupportTestCase extends AbstractExtensionFunctionalTestCase {

  private final String config;
  @Rule
  public ExpectedException expectedException = none();

  public ExpressionSupportTestCase(String config) {
    this.config = config;
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"heisenberg-invalid-expression-parameter.xml"},
        {"heisenberg-fixed-parameter-with-expression.xml"}, {"heisenberg-invalid-expression-parameter-inside-group.xml"}});
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {config};
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectCause(instanceOf(IllegalArgumentException.class));
    expectedException.expectCause(new TypeSafeMatcher<Throwable>() {

      @Override
      protected boolean matchesSafely(Throwable exception) {
        return exception.getMessage().contains("expressions");
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("exception cause was expected to have the word expressions");
      }
    });
  }

  @Test
  public void invalidConfig() throws Exception {
    fail("Configuration should have been invalid");
  }
}
