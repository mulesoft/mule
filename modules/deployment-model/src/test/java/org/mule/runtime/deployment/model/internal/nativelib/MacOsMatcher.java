/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.nativelib;

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
