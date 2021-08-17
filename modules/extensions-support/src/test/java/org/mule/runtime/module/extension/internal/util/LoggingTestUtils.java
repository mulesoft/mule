/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.util.Arrays.copyOfRange;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

public class LoggingTestUtils {

  private LoggingTestUtils() {}

  public static Logger createMockLogger(List<String> debugMessageList, List<String> traceMessageList) {
    Logger logger = mock(Logger.class, withSettings().lenient());
    Answer answer = invocation -> {
      String method = invocation.getMethod().getName();
      String message = invocation.getArgument(0, String.class);
      Object[] messageArgs = copyOfRange(invocation.getArguments(), 1, invocation.getArguments().length);
      if (method.equals("debug")) {
        debugMessageList.add(formatMessage(message, messageArgs));
      } else if (method.equals("trace")) {
        traceMessageList.add(formatMessage(message, messageArgs));
      }
      return null;
    };
    doAnswer(answer).when(logger).debug(anyString(), (Object) any());
    doAnswer(answer).when(logger).debug(anyString(), any(), any());
    doAnswer(answer).when(logger).debug(anyString(), (Object[]) any());
    doAnswer(answer).when(logger).trace(anyString(), any(), any());
    doAnswer(answer).when(logger).trace(anyString(), (Object[]) any());
    when(logger.isDebugEnabled()).thenReturn(true);
    when(logger.isTraceEnabled()).thenReturn(true);
    return logger;
  }

  private static String formatMessage(String message, Object... args) {
    String newMessage = message.replaceAll("\\{\\}", "%s");
    return String.format(newMessage, args);
  }

  public static Logger setLogger(Object object, String fieldName, Logger newLogger) throws Exception {
    Field field = object.getClass().getDeclaredField(fieldName);
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

}
