/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming;

import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.mule.runtime.api.util.MuleSystemProperties.TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

@RunWith(Parameterized.class)
public abstract class AbstractTroubleshootCursorProviderTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Parameter
  public Boolean trackStackTrace;

  @Parameter(1)
  public boolean setComponentLocation;

  private CursorProvider cursorProvider;

  protected ComponentLocation componentLocation;

  @Parameters(name = "Track StackTrace: {0}, set ComponentLocation: {1}")
  public static Object[] getParameters() {
    return new Object[][] {
        {false, false},
        {false, true},
        {true, false},
        {true, true}
    };
  }

  @Before
  public void before() throws NoSuchFieldException, IllegalAccessException {
    setProperty(TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY, trackStackTrace.toString());

    componentLocation = setComponentLocation ? fromSingleComponent("log") : null;

    cursorProvider = createCursorProvider();
  }

  @After
  public void after() {
    cursorProvider.close();
  }

  @Test
  @Issue("MULE-18047")
  @Description("The cursor provider should have a component location reference")
  public void hasComponentLocation() {
    if (setComponentLocation) {
      assertThat(cursorProvider.getOriginatingLocation().orElse(null), is(notNullValue()));
    } else {
      assertThat(cursorProvider.getOriginatingLocation().orElse(null), is(nullValue()));
    }
  }

  @Test
  @Issue("MULE-18047")
  @Description("Verifies which exception is thrown when trying to open a closed cursor provided")
  public void failIfProviderClosed() {
    expectedException.expect(CursorProviderAlreadyClosedException.class);
    expectedException.expectMessage(containsString("Cannot open a new cursor on a closed"));
    expectedException.expectMessage(containsString("The cursor provider was open by"));
    if (trackStackTrace) {
      expectedException.expectMessage(containsString("The cursor provider was closed by"));
    } else {
      expectedException.expectMessage(containsString("for more details"));
    }

    cursorProvider.close();

    cursorProvider.openCursor();
  }

  protected abstract CursorProvider createCursorProvider();

}
