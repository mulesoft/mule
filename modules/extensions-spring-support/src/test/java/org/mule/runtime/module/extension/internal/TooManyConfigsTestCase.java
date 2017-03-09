/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.rules.ExpectedException.none;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TooManyConfigsTestCase extends AbstractImplicitExclusiveConfigTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String getConfigFile() {
    return "too-many-configs.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectCause(instanceOf(LifecycleException.class));
    expectedException
        .expectMessage("Too many configs of type 'blaconf' were found");
  }

  @Test
  public void tooManyConfigsTestCase() throws Exception {
    flowRunner("implicitConfig").run();
  }
}
