/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static java.util.Arrays.copyOfRange;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public class LoggingTestUtils {

  private LoggingTestUtils() {}

  public static Logger createMockLogger(List<String> messageList, Level level) {
    Logger logger = mock(Logger.class, withSettings().lenient());
    Answer answer = invocation -> {
      String message = invocation.getArgument(0, String.class);
      Object[] messageArgs = copyOfRange(invocation.getArguments(), 1, invocation.getArguments().length);
      messageList.add(formatMessage(message, messageArgs));
      return null;
    };
    switch (level) {
      case ERROR:
        when(logger.isErrorEnabled()).thenReturn(true);
        doAnswer(answer).when(logger).error(anyString());
        doAnswer(answer).when(logger).error(anyString(), (Object) any());
        doAnswer(answer).when(logger).error(anyString(), (Throwable) any());
        doAnswer(answer).when(logger).error(anyString(), any(), any());
        doAnswer(answer).when(logger).error(anyString(), (Object[]) any());
        break;
      case WARN:
        when(logger.isWarnEnabled()).thenReturn(true);
        doAnswer(answer).when(logger).warn(anyString());
        doAnswer(answer).when(logger).warn(anyString(), (Object) any());
        doAnswer(answer).when(logger).warn(anyString(), any(), any());
        doAnswer(answer).when(logger).warn(anyString(), (Object[]) any());
        break;
      case INFO:
        when(logger.isInfoEnabled()).thenReturn(true);
        doAnswer(answer).when(logger).info(anyString());
        doAnswer(answer).when(logger).info(anyString(), (Object) any());
        doAnswer(answer).when(logger).info(anyString(), any(), any());
        doAnswer(answer).when(logger).info(anyString(), (Object[]) any());
        break;
      case DEBUG:
        when(logger.isDebugEnabled()).thenReturn(true);
        doAnswer(answer).when(logger).debug(anyString());
        doAnswer(answer).when(logger).debug(anyString(), (Object) any());
        doAnswer(answer).when(logger).debug(anyString(), any(), any());
        doAnswer(answer).when(logger).debug(anyString(), (Object[]) any());
        break;
      case TRACE:
        when(logger.isTraceEnabled()).thenReturn(true);
        doAnswer(answer).when(logger).trace(anyString());
        doAnswer(answer).when(logger).trace(anyString(), (Object) any());
        doAnswer(answer).when(logger).trace(anyString(), any(), any());
        doAnswer(answer).when(logger).trace(anyString(), (Object[]) any());
        break;
    }
    return logger;
  }

  private static String formatMessage(String message, Object... args) {
    assertThat("Log messages must use '{}' instead of '%s'", message, not(containsString("%s")));
    String newMessage = message.replaceAll("\\{\\}", "%s");
    return String.format(newMessage, args);
  }

  public static Logger setLogger(Object object, String fieldName, Logger newLogger) throws Exception {
    return setLogger(object.getClass(), fieldName, newLogger);
  }

  public static Logger setLogger(Class clazz, String fieldName, Logger newLogger) throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    Logger oldLogger;
    try {
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
      try {
        oldLogger = (Logger) field.get(null);
        field.set(null, newLogger);
      } finally {
        // undo accessibility changes
        modifiersField.setInt(field, field.getModifiers());
        modifiersField.setAccessible(false);
      }
    } finally {
      // undo accessibility changes
      field.setAccessible(false);
    }
    return oldLogger;
  }

  public static void verifyLogMessage(List<String> messages, String expectedMessage, Object... arguments) {
    assertThat(messages, hasItem(formatMessage(expectedMessage, arguments)));
  }

  public static void verifyLogRegex(List<String> messages, String expectedRegex, Object... arguments) {
    assertThat(messages, hasItem(new RegexMatcher(formatMessage(expectedRegex, arguments))));
  }

  private static class RegexMatcher extends TypeSafeMatcher<String> {

    private final String regex;

    public RegexMatcher(final String regex) {
      this.regex = regex;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("matches regex=`" + regex + "`");
    }

    @Override
    public boolean matchesSafely(final String string) {
      return string.matches(regex);
    }
  }

}
