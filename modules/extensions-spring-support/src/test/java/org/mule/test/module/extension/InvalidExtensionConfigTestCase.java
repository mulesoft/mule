/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import org.mule.runtime.api.lifecycle.InitialisationException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * An {@link AbstractExtensionFunctionalTestCase} which is expected to point to a somewhat invalid config. The test fails if the config is
 * parsed correctly.
 * <p>
 * This class does not require to implement any method annotation with {@link Test}
 *
 * @since 4.0
 */
public abstract class InvalidExtensionConfigTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    configureExceptionAssertions();
  }

  private void configureExceptionAssertions() {
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    expectedException.expect(InitialisationException.class);
    additionalExceptionAssertions(expectedException);
  }

  protected void additionalExceptionAssertions(ExpectedException expectedException) {}

  @Test
  public void fail() {
    Assert.fail("Config should have failed to parse");
  }
}
