/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.logger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.List;

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
    addMessage("[TRACE] " + msg);
  }

  // Implement other trace methods similarly

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public void debug(String msg) {
    addMessage(msg);
  }

  // Implement other debug methods similarly

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public void trace(String s, Object o) {

  }

  @Override
  public void trace(String s, Object o, Object o1) {

  }

  @Override
  public void trace(String s, Object... objects) {

  }

  @Override
  public void trace(String s, Throwable throwable) {

  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return false;
  }

  @Override
  public void trace(Marker marker, String s) {

  }

  @Override
  public void trace(Marker marker, String s, Object o) {

  }

  @Override
  public void trace(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void trace(Marker marker, String s, Object... objects) {

  }

  @Override
  public void trace(Marker marker, String s, Throwable throwable) {

  }

  @Override
  public void debug(String s, Object o) {
    addMessage(s + o.toString());
  }

  @Override
  public void debug(String s, Object o, Object o1) {
    String msg = StringUtils.replaceOnce(s, "{}", o.toString());
    msg = StringUtils.replaceOnce(msg, "{}", o1.toString());
    addMessage(msg);
  }

  @Override
  public void debug(String s, Object... objects) {

  }

  @Override
  public void debug(String s, Throwable throwable) {

  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return false;
  }

  @Override
  public void debug(Marker marker, String s) {

  }

  @Override
  public void debug(Marker marker, String s, Object o) {

  }

  @Override
  public void debug(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void debug(Marker marker, String s, Object... objects) {

  }

  @Override
  public void debug(Marker marker, String s, Throwable throwable) {

  }

  @Override
  public void info(String s) {

  }

  @Override
  public void info(String s, Object o) {

  }

  @Override
  public void info(String s, Object o, Object o1) {

  }

  @Override
  public void info(String s, Object... objects) {

  }

  @Override
  public void info(String s, Throwable throwable) {

  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return false;
  }

  @Override
  public void info(Marker marker, String s) {

  }

  @Override
  public void info(Marker marker, String s, Object o) {

  }

  @Override
  public void info(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void info(Marker marker, String s, Object... objects) {

  }

  @Override
  public void info(Marker marker, String s, Throwable throwable) {

  }

  @Override
  public boolean isWarnEnabled() {
    return false;
  }

  @Override
  public void warn(String s) {

  }

  @Override
  public void warn(String s, Object o) {

  }

  @Override
  public void warn(String s, Object... objects) {

  }

  @Override
  public void warn(String s, Object o, Object o1) {

  }

  @Override
  public void warn(String s, Throwable throwable) {

  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return false;
  }

  @Override
  public void warn(Marker marker, String s) {

  }

  @Override
  public void warn(Marker marker, String s, Object o) {

  }

  @Override
  public void warn(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void warn(Marker marker, String s, Object... objects) {

  }

  @Override
  public void warn(Marker marker, String s, Throwable throwable) {

  }

  @Override
  public boolean isErrorEnabled() {
    return false;
  }

  @Override
  public void error(String s) {

  }

  @Override
  public void error(String s, Object o) {

  }

  @Override
  public void error(String s, Object o, Object o1) {

  }

  @Override
  public void error(String s, Object... objects) {

  }

  @Override
  public void error(String s, Throwable throwable) {

  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return false;
  }

  @Override
  public void error(Marker marker, String s) {

  }

  @Override
  public void error(Marker marker, String s, Object o) {

  }

  @Override
  public void error(Marker marker, String s, Object o, Object o1) {

  }

  @Override
  public void error(Marker marker, String s, Object... objects) {

  }

  @Override
  public void error(Marker marker, String s, Throwable throwable) {

  }
}
