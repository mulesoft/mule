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
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MultipleImplicitConfigTestCase extends AbstractImplicitExclusiveConfigTestCase {


  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {MultipleImplicitConfigExtension.class};
  }

  @Extension(name = "implicit")
  @Xml(namespace = "http://www.mulesoft.org/schema/mule/implicit", prefix = "implicit")
  @Configurations(value = {BlaConfig.class, BleConfig.class, NonImplicitConfig.class, AnotherConfigThatCanBeUsedImplicitly.class})
  public static class MultipleImplicitConfigExtension {
  }

  @Operations({BleOperations.class})
  @Configuration(name = "yetanotherimplicitconfig")
  public static class AnotherConfigThatCanBeUsedImplicitly extends ConfigWithNumber {

    @Override
    int getNumber() {
      return 20;
    }
  }

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
    expectedException
        .expectMessage("No configuration can be inferred por extension 'implicit'");
  }

  @Test
  public void moreThanOneImplicitConfigAvailableTestCase() throws Exception {
    flowRunner("implicitConfig").run();
  }
}
