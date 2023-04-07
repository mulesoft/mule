/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.logger;

import static java.util.stream.Collectors.joining;
import static java.util.Arrays.stream;

import static org.apache.commons.lang3.StringUtils.replaceOnce;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class CustomLogger implements Logger {

  private final String name;
  private List<String> messages;

  public CustomLogger(String name) {
    this.name = name;
    this.messages = new ArrayList<>();
  }

  public void resetLogs() {
    this.messages = new ArrayList<>();
  }

  public List<String> getMessages() {
    return messages;
  }

  private void addMessage(String message) {
    messages.add(message);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isTraceEnabled() {
    return true;
  }

  @Override
  public void trace(String msg) {
    addMessage(msg);
  }

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public void debug(String msg) {
    addMessage(msg);
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public void trace(String s, Object o) {
    addMessage(replaceOnce(s, "{}", o.toString()));
  }

  @Override
  public void trace(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
  }

  @Override
  public void trace(String s, Object... objects) {
    String updatedStr = s;
    for (Object object : objects) {
      updatedStr = replaceOnce(updatedStr, "{}", object.toString());
    }
    addMessage(updatedStr);
  }

  @Override
  public void trace(String s, Throwable throwable) {
    addMessage("[TRACE] " + s + throwable.getMessage());
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return false;
  }

  @Override
  public void trace(Marker marker, String s) {
    throw new UnsupportedOperationException("trace with marker not suppported");
  }

  @Override
  public void trace(Marker marker, String s, Object o) {
    throw new UnsupportedOperationException("trace with marker not suppported");
  }

  @Override
  public void trace(Marker marker, String s, Object o, Object o1) {
    throw new UnsupportedOperationException("trace with marker not suppported");
  }

  @Override
  public void trace(Marker marker, String s, Object... objects) {
    throw new UnsupportedOperationException("trace with marker not suppported");
  }

  @Override
  public void trace(Marker marker, String s, Throwable throwable) {
    throw new UnsupportedOperationException("trace with marker not suppported");
  }

  @Override
  public void debug(String s, Object o) {
    addMessage(s + o.toString());
  }

  @Override
  public void debug(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
  }

  @Override
  public void debug(String s, Object... objects) {
    String updatedStr = s;
    for (Object object : objects) {
      updatedStr = replaceOnce(updatedStr, "{}", object.toString());
    }
    addMessage(updatedStr);
  }

  @Override
  public void debug(String s, Throwable throwable) {
    addMessage("[DEBUG] " + s + throwable.getMessage());
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return false;
  }

  @Override
  public void debug(Marker marker, String s) {
    throw new UnsupportedOperationException("debug with marker not suppported");
  }

  @Override
  public void debug(Marker marker, String s, Object o) {
    throw new UnsupportedOperationException("debug with marker not suppported");
  }

  @Override
  public void debug(Marker marker, String s, Object o, Object o1) {
    addMessage("[DEBUG] " + s + o + o1);
  }

  @Override
  public void debug(Marker marker, String s, Object... objects) {
    addMessage("[DEBUG] " + s + stringify(objects));
  }

  @Override
  public void debug(Marker marker, String s, Throwable throwable) {
    throw new UnsupportedOperationException("debug with marker not suppported");
  }

  @Override
  public void info(String s) {
    addMessage("[INFO] " + s);
  }

  @Override
  public void info(String s, Object o) {
    addMessage("[INFO] " + s + o);
  }

  @Override
  public void info(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
  }

  @Override
  public void info(String s, Object... objects) {
    addMessage("[INFO] " + s + stringify(objects));
  }

  @Override
  public void info(String s, Throwable throwable) {
    addMessage(s + throwable);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return false;
  }

  @Override
  public void info(Marker marker, String s) {
    throw new UnsupportedOperationException("info with marker not suppported");
  }

  @Override
  public void info(Marker marker, String s, Object o) {
    throw new UnsupportedOperationException("info with marker not suppported");
  }

  @Override
  public void info(Marker marker, String s, Object o, Object o1) {
    throw new UnsupportedOperationException("info with marker not suppported");
  }

  @Override
  public void info(Marker marker, String s, Object... objects) {
    throw new UnsupportedOperationException("info with marker not suppported");
  }

  @Override
  public void info(Marker marker, String s, Throwable throwable) {
    throw new UnsupportedOperationException("info with marker not suppported");
  }

  @Override
  public boolean isWarnEnabled() {
    return false;
  }

  @Override
  public void warn(String s) {
    addMessage(s);
  }

  @Override
  public void warn(String s, Object o) {
    addMessage(s + o);
  }

  @Override
  public void warn(String s, Object... objects) {
    addMessage(s + stringify(objects));
  }

  @Override
  public void warn(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
  }

  @Override
  public void warn(String s, Throwable throwable) {
    addMessage(s + throwable);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return false;
  }

  @Override
  public void warn(Marker marker, String s) {
    throw new UnsupportedOperationException("warn with marker not suppported");
  }

  @Override
  public void warn(Marker marker, String s, Object o) {
    throw new UnsupportedOperationException("warn with marker not suppported");
  }

  @Override
  public void warn(Marker marker, String s, Object o, Object o1) {
    throw new UnsupportedOperationException("warn with marker not suppported");
  }

  @Override
  public void warn(Marker marker, String s, Object... objects) {
    addMessage(s + stringify(objects));
  }

  @Override
  public void warn(Marker marker, String s, Throwable throwable) {
    throw new UnsupportedOperationException("warn with marker not suppported");
  }

  @Override
  public boolean isErrorEnabled() {
    return false;
  }

  @Override
  public void error(String s) {
    addMessage(s);
  }

  @Override
  public void error(String s, Object o) {
    addMessage(s + o);
  }

  @Override
  public void error(String s, Object o, Object o1) {
    addMessage(s + o + o1);
  }

  @Override
  public void error(String s, Object... objects) {
    addMessage(s + stringify(objects));
  }

  @Override
  public void error(String s, Throwable throwable) {
    addMessage(s + throwable.getMessage());
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return false;
  }

  @Override
  public void error(Marker marker, String s) {
    throw new UnsupportedOperationException("error with marker not suppported");
  }

  @Override
  public void error(Marker marker, String s, Object o) {
    throw new UnsupportedOperationException("error with marker not suppported");
  }

  @Override
  public void error(Marker marker, String s, Object o, Object o1) {
    throw new UnsupportedOperationException("error with marker not suppported");
  }

  @Override
  public void error(Marker marker, String s, Object... objects) {
    throw new UnsupportedOperationException("error with marker not suppported");
  }

  @Override
  public void error(Marker marker, String s, Throwable throwable) {
    throw new UnsupportedOperationException("error with marker not suppported");
  }

  private String stringify(Object[] objects) {
    return stream(objects).map(a -> a.toString()).collect(joining(","));
  }
}
