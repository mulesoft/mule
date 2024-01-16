/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

public class ReconfigurableOnApplicationLogger extends Logger {

  private final Runnable onLoggingAction;

  public ReconfigurableOnApplicationLogger(LoggerContext ctx, String name, MessageFactory messageFactory,
                                           Runnable onLoggingAction) {
    super(ctx, name, messageFactory);
    this.onLoggingAction = onLoggingAction;
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Message msg, Throwable t) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, msg, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Object message, Throwable t) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object... params) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, params);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Throwable t) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, MessageSupplier msgSupplier, Throwable t) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, msgSupplier, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, CharSequence message, Throwable t) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Supplier<?> msgSupplier, Throwable t) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, msgSupplier, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Supplier<?>... paramSuppliers) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, paramSuppliers);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7, Object p8) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
    onLoggingAction.run();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }
}
