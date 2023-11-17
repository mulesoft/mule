/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.MessageFactory;

public class LoggerWrapper extends Logger {

  public static final String FILE_APPENDER_NAME = "file";

  public LoggerWrapper(LoggerContext ctx, String name, MessageFactory messageFactory, Appender fileAppender) {
    super(ctx, name, messageFactory);
    Appender previousFileAppender = getAppenders().get(FILE_APPENDER_NAME);
    if (previousFileAppender != null) {
      removeAppender(previousFileAppender);
    }
    addAppender(fileAppender);
  }
}
