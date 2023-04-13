/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.logger;

import static org.apache.commons.lang3.StringUtils.replaceOnce;
import static org.slf4j.event.Level.TRACE;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * In some tests we were replacing the original logger in a class with a mocked logger via reflection and then setting it back to
 * the original. This reflective access will not work in Java 17, hence writing a custom logger that will be used with specific
 * tests.This Logger relies on caller setting the correct log level in Test setup. So before using the class you need to call
 * setlevel(Level) and clear the log level at the end, which would then default to log level of original underlying logger.
 */
public class CustomLogger implements Logger {

  private final String name;
  private List<String> messages;
  private final Logger logger;
  private Level level = WARN;

  public CustomLogger(Logger logger, String name) {
    this.name = name;
    this.messages = new ArrayList<>();
    this.logger = logger;
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

  public void setLevel(Level level) {
    this.level = level;
  }

  /**
   * After test runs this should be called and restores log level to original logger's Level
   */
  public void resetLevel() {
    if (logger.isInfoEnabled()) {
      level = INFO;
    } else if (logger.isDebugEnabled()) {
      level = DEBUG;
    } else if (logger.isTraceEnabled()) {
      level = TRACE;
    } else if (logger.isErrorEnabled()) {
      level = ERROR;
    } else if (logger.isWarnEnabled()) {
      level = WARN;
    } else {
      level = INFO;
    }
  }

  @Override
  public boolean isTraceEnabled() {
    return level == TRACE;
  }

  @Override
  public void trace(String msg) {
    addMessage(msg);
    logger.trace(msg);
  }

  @Override
  public boolean isDebugEnabled() {
    return level == DEBUG;
  }

  @Override
  public void debug(String msg) {
    addMessage(msg);
    logger.debug(msg);
  }

  @Override
  public boolean isInfoEnabled() {
    return level == INFO;
  }

  @Override
  public void trace(String s, Object o) {
    addMessage(replaceOnce(s, "{}", o.toString()));
    logger.trace(s, o);
  }

  @Override
  public void trace(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
    logger.trace(s, o, o1);
  }

  @Override
  public void trace(String s, Object... objects) {
    addMessage(buildLogMessage(s, objects));
    logger.trace(s, objects);
  }

  @Override
  public void trace(String s, Throwable throwable) {
    addMessage(s + throwable.getMessage());
    logger.trace(s, throwable);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return level == TRACE;
  }

  @Override
  public void trace(Marker marker, String s) {
    logger.trace(marker, s);
  }

  @Override
  public void trace(Marker marker, String s, Object o) {
    logger.trace(marker, s, o);
  }

  @Override
  public void trace(Marker marker, String s, Object o, Object o1) {
    logger.trace(marker, s, o, o1);
  }

  @Override
  public void trace(Marker marker, String s, Object... objects) {
    logger.trace(marker, s, objects);
  }

  @Override
  public void trace(Marker marker, String s, Throwable throwable) {
    logger.trace(marker, s, throwable);
  }

  @Override
  public void debug(String s, Object o) {
    addMessage(replaceOnce(s, "{}", o.toString()));
    logger.debug(s, o);
  }

  @Override
  public void debug(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
    logger.debug(s, o, o1);
  }

  @Override
  public void debug(String s, Object... objects) {
    addMessage(buildLogMessage(s, objects));
    logger.debug(s, objects);
  }

  @Override
  public void debug(String s, Throwable throwable) {
    addMessage(s + throwable.getMessage());
    logger.debug(s, throwable);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return logger.isDebugEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String s) {
    logger.debug(marker, s);
  }

  @Override
  public void debug(Marker marker, String s, Object o) {
    logger.debug(marker, s, o);
  }

  @Override
  public void debug(Marker marker, String s, Object o, Object o1) {
    logger.debug(marker, s, o, o1);
  }

  @Override
  public void debug(Marker marker, String s, Object... objects) {
    logger.debug(marker, s, objects);
  }

  @Override
  public void debug(Marker marker, String s, Throwable throwable) {
    logger.debug(marker, s, throwable);
  }

  @Override
  public void info(String s) {
    addMessage(s);
    logger.info(s);
  }

  @Override
  public void info(String s, Object o) {
    addMessage(replaceOnce(s, "{}", o.toString()));
    logger.info(s, o);
  }

  @Override
  public void info(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
    logger.info(s, o, o1);
  }

  @Override
  public void info(String s, Object... objects) {
    addMessage(buildLogMessage(s, objects));
    logger.info(s, objects);
  }

  @Override
  public void info(String s, Throwable throwable) {
    logger.info(s, throwable);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return logger.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String s) {
    logger.info(marker, s);
  }

  @Override
  public void info(Marker marker, String s, Object o) {
    logger.info(marker, s, o);
  }

  @Override
  public void info(Marker marker, String s, Object o, Object o1) {
    logger.info(marker, s, o, o1);
  }

  @Override
  public void info(Marker marker, String s, Object... objects) {
    logger.info(marker, s, objects);
  }

  @Override
  public void info(Marker marker, String s, Throwable throwable) {
    logger.info(marker, s, throwable);
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public void warn(String s) {
    addMessage(s);
    logger.warn(s);
  }

  @Override
  public void warn(String s, Object o) {
    addMessage(replaceOnce(s, "{}", o.toString()));
    logger.warn(s, o);
  }

  @Override
  public void warn(String s, Object... objects) {
    addMessage(buildLogMessage(s, objects));
    logger.warn(s, objects);
  }

  public void warn(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
    logger.warn(s, o, o1);
  }


  @Override
  public void warn(String s, Throwable throwable) {
    addMessage(s + throwable);
    logger.warn(s, throwable);
  }

  @Override

  public boolean isWarnEnabled(Marker marker) {
    return logger.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String s) {
    logger.warn(marker, s);
  }

  @Override
  public void warn(Marker marker, String s, Object o) {
    logger.warn(marker, s, o);
  }

  @Override
  public void warn(Marker marker, String s, Object o, Object o1) {
    logger.warn(marker, s, o, o1);
  }

  @Override
  public void warn(Marker marker, String s, Object... objects) {
    addMessage(buildLogMessage(s, objects));
    logger.warn(marker, s, objects);
  }

  @Override
  public void warn(Marker marker, String s, Throwable throwable) {
    logger.warn(marker, s, throwable);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public void error(String s) {
    addMessage(s);
    logger.error(s);
  }

  @Override
  public void error(String s, Object o) {
    addMessage(replaceOnce(s, "{}", o.toString()));
    logger.error(s, o);
  }

  @Override
  public void error(String s, Object o, Object o1) {
    String msg = replaceOnce(s, "{}", o.toString());
    msg = replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
    logger.error(s, o, o1);
  }

  @Override
  public void error(String s, Object... objects) {
    addMessage(buildLogMessage(s, objects));
    logger.error(s, objects);
  }

  @Override
  public void error(String s, Throwable throwable) {
    addMessage(s + throwable.getMessage());
    logger.error(s, throwable);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return logger.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String s) {
    logger.error(marker, s);
  }

  @Override
  public void error(Marker marker, String s, Object o) {
    logger.error(marker, s, o);
  }

  @Override
  public void error(Marker marker, String s, Object o, Object o1) {
    logger.error(marker, s, o1);
  }

  @Override
  public void error(Marker marker, String s, Object... objects) {
    logger.error(marker, s, objects);
  }

  @Override
  public void error(Marker marker, String s, Throwable throwable) {
    logger.error(marker, s, throwable);
  }

  private String buildLogMessage(String s, Object... objects) {
    String updatedStr = s;
    for (Object object : objects) {
      updatedStr = replaceOnce(updatedStr, "{}", object.toString());
    }
    return updatedStr;
  }
}
