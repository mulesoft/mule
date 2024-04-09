/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.module.boot.commons.internal.MuleContainerBasicWrapper;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Issue;

@SmallTest
public class MuleContainerBasicWrapperTestCase {

  @Rule
  public ExpectedException expected = none();

  @Test
  @Issue("W-14742344")
  public void haltAndCatchFireNoError() {
    new MuleContainerBasicWrapper().haltAndCatchFire(0, null);
    // no exception thrown
  }

  @Test
  public void haltAndCatchFireError() {
    final String errorMessage = "error message";
    expected.expectMessage(errorMessage);
    new MuleContainerBasicWrapper().haltAndCatchFire(-1, errorMessage);
  }
}
