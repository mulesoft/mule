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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

/**
 * A {@link Logger} that performs an extra action everytime it logs.
 *
 * @since 4.7.0
 */
class LoggerWithReconfigurationAction extends Logger {

  private final LoggerReconfigurationAction reconfigurationAction;
  private boolean reconfigured;

  public LoggerWithReconfigurationAction(LoggerContext ctx, String name, MessageFactory messageFactory,
                                         LoggerReconfigurationAction reconfigurationAction) {
    super(ctx, name, messageFactory);
    this.reconfigurationAction = reconfigurationAction;
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Message msg, Throwable t) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, msg, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Object message, Throwable t) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object... params) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, params);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Throwable t) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, MessageSupplier msgSupplier, Throwable t) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, msgSupplier, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, CharSequence message, Throwable t) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, Supplier<?> msgSupplier, Throwable t) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, msgSupplier, t);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Supplier<?>... paramSuppliers) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, paramSuppliers);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7, Object p8) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
  }

  @Override
  public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3,
                           Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
    reconfigureIfNeeded();
    super.logIfEnabled(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
  }

  private void reconfigureIfNeeded() {
    if (!reconfigured) {
      reconfigured = reconfigurationAction.reconfigure();
    }
  }
}
