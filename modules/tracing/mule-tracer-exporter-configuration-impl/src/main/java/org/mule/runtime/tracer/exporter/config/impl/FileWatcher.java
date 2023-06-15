/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.config.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.io.File;

/**
 * A watcher for changes in the configuration file
 */
public class FileWatcher extends Thread {

  private static final Logger LOGGER = getLogger(FileWatcher.class);
  public static final long DEFAULT_DELAY = 60000L;
  private final String filename;
  private final Runnable doOnChange;

  protected long delay = DEFAULT_DELAY;
  File file;
  long lastModified;
  boolean warnedAlready;
  boolean interrupted;

  public FileWatcher(String filename, Runnable doOnChange) {
    super("FileSpanExporterConfigurationWatcher");
    this.filename = filename;
    this.file = new File(filename);
    this.doOnChange = doOnChange;
    this.setDaemon(true);
    this.checkAndConfigure();
  }

  protected void checkAndConfigure() {
    boolean fileExists;
    try {
      fileExists = this.file.exists();
    } catch (SecurityException var4) {
      LOGGER.warn("Was not allowed to read check file existance, file:[" + this.filename + "].");
      this.interrupted = true;
      return;
    }

    if (fileExists) {
      long fileLastMod = this.file.lastModified();
      if (fileLastMod > this.lastModified) {
        this.lastModified = fileLastMod;
        this.doOnChange();
        this.warnedAlready = false;
      }
    } else if (!this.warnedAlready) {
      LOGGER.warn("Configuration for file exporter was not found. It was possibly removed.");
      this.warnedAlready = true;
    }
  }

  private void doOnChange() {
    doOnChange.run();
  }
}
