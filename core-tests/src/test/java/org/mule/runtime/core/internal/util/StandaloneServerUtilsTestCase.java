/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_BASE_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.internal.util.StandaloneServerUtils.getMuleBase;
import static org.mule.runtime.core.internal.util.StandaloneServerUtils.getMuleHome;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class StandaloneServerUtilsTestCase extends AbstractMuleTestCase {

  private static final String EXPECTED_MULE_HOME_VALUE = "expected-mule-home-value";
  private static final String EXPECTED_MULE_BASE_VALUE = "expected-mule-base-value";

  @Test
  public void muleHome() throws Exception {
    testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, EXPECTED_MULE_HOME_VALUE,
                           () -> assertThat(getMuleHome().get().getName(), is(EXPECTED_MULE_HOME_VALUE)));
  }

  @Test
  public void muleHomeIsNullWhenNotDefined() throws Exception {
    assertThat(getMuleHome().isPresent(), is(false));
  }

  @Test
  public void muleBase() throws Exception {
    testWithSystemProperty(MULE_BASE_DIRECTORY_PROPERTY, EXPECTED_MULE_BASE_VALUE,
                           () -> assertThat(getMuleBase().get().getName(), is(EXPECTED_MULE_BASE_VALUE)));
  }

  @Test
  public void muleBaseReturnsMuleHomeWhenNotSet() throws Exception {
    testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, EXPECTED_MULE_HOME_VALUE,
                           () -> assertThat(getMuleBase().get().getName(), is(EXPECTED_MULE_HOME_VALUE)));
  }

  @Test
  public void muleBaseReturnsNullIfNetherMuleHomeOrMuleBaseIsSet() throws Exception {
    assertThat(getMuleBase().isPresent(), is(false));
  }
}
