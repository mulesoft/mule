/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.qameta.allure.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests for the {@link FunctionalTestCase} support class when used with and without lazy initialization.
 */
@RunWith(Parameterized.class)
public class FunctionalTestCaseWithLazyInitTestCase extends FunctionalTestCase {

  private final boolean enableLazyInit;

  @Parameterized.Parameters(name = "enableLazyInit: {0} ")
  public static Object[] data() {
    return new Object[] {true, false};
  }

  public FunctionalTestCaseWithLazyInitTestCase(boolean enableLazyInit) {
    this.enableLazyInit = enableLazyInit;
  }

  @Override
  public boolean enableLazyInit() {
    return enableLazyInit;
  }

  @Override
  protected String getConfigFile() {
    return "dummy-mule-app.xml";
  }

  @Test
  @Description("Checks that the min Mule version is present in the MuleConfiguration")
  public void minMuleVersionHasBeenSet() {
    assertThat(muleContext.getConfiguration().getMinMuleVersion().isPresent(), is(true));
  }
}
