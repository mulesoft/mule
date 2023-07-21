/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.nativelib;

import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class MacOsMatcher extends BaseMatcher<AbstractMuleTestCase> {

  @Override
  public boolean matches(Object o) {
    return IS_OS_MAC;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("Need a Mac Os to run");
  }
}
