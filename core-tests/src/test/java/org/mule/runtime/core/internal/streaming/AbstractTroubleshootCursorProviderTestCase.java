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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.*;
import static org.mule.runtime.api.util.MuleSystemProperties.TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

@RunWith(Parameterized.class)
public abstract class AbstractTroubleshootCursorProviderTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public MockitoRule mockitoRule = rule();

  @Parameter
  public Boolean trackStackTrace;

  @Parameter(1)
  public boolean setComponentLocation;

  @Mock
  protected EventContext eventContext;

  @Mock
  protected StreamingManager streamingManager;

  private CursorProvider cursorProvider;

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

    when(eventContext.getOriginatingLocation()).then(a -> setComponentLocation ? fromSingleComponent("log") : null);

    when(streamingManager.manage(any(CursorProvider.class), any(EventContext.class))).then(a -> a.getArgument(0));

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

  private void setStaticField(Class cls, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
    Field field = cls.getDeclaredField(fieldName);
    Field modifiers = Field.class.getDeclaredField("modifiers");

    boolean wasAccessible = field.isAccessible();

    modifiers.setAccessible(true);
    field.setAccessible(true);

    int prevModifiers = modifiers.getInt(field);

    modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(null, value);

    modifiers.setInt(field, prevModifiers);
    modifiers.setAccessible(false);
    field.setAccessible(wasAccessible);
  }
}
