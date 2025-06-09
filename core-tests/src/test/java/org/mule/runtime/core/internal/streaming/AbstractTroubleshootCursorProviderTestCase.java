/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

@RunWith(Parameterized.class)
public abstract class AbstractTroubleshootCursorProviderTestCase extends AbstractMuleTestCase {

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
    componentLocation = setComponentLocation ? from("log") : null;

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
    cursorProvider.close();

    var thrown = assertThrows(CursorProviderAlreadyClosedException.class, () -> cursorProvider.openCursor());
    assertThat(thrown.getMessage(), containsString("Cannot open a new cursor on a closed"));
    assertThat(thrown.getMessage(), containsString("The cursor provider was open by"));
    if (trackStackTrace) {
      assertThat(thrown.getMessage(), containsString("The cursor provider was closed by"));
      assertThat(thrown.getMessage(), containsString("ClosingCursorException"));
    } else {
      assertThat(thrown.getMessage(), containsString("for more details"));
    }
  }

  protected abstract CursorProvider createCursorProvider();

}
