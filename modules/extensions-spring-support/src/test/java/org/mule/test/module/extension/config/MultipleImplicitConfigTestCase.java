/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.rules.ExpectedException.none;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MultipleImplicitConfigTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String getConfigFile() {
    return "multiple-implicit-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectCause(instanceOf(LifecycleException.class));
    expectedException.expectMessage("No configuration can be inferred for extension 'multiImplicitConfig'");
  }

  @Test
  public void moreThanOneImplicitConfigAvailableTestCase() throws Exception {
    flowRunner("implicitConfig").run();
  }
}
