/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.common.watcher;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CONFIGURATION_WATCHER_DEFAULT_DELAY_PROPERTY;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties;

import java.io.File;

import org.slf4j.Logger;

/**
 * A watcher for changes in the configuration file.
 */
public class TracingConfigurationFileWatcher extends Thread {

  private static final Logger LOGGER = getLogger(TracingConfigurationFileWatcher.class);
  public final long DEFAULT_DELAY = Long
      .getLong(MULE_OPEN_TELEMETRY_EXPORTER_CONFIGURATION_WATCHER_DEFAULT_DELAY_PROPERTY,
               60000l);
  private final String filename;
  private final Runnable doOnChange;

  protected long delay = DEFAULT_DELAY;
  File file;
  long lastModified;
  boolean warnedAlready;
  boolean interrupted;

  public TracingConfigurationFileWatcher(String filename, Runnable doOnChange) {
    super("FileSpanExporterConfigurationWatcher");
    this.filename = filename;
    this.file = new File(filename);
    this.doOnChange = doOnChange;
    this.lastModified = file.lastModified();
    this.setDaemon(true);
  }

  protected void checkAndConfigure() {
    boolean fileExists;
    try {
      fileExists = file.exists();
    } catch (SecurityException var4) {
      LOGGER.warn("The tracing config file " + filename + " was possibly removed.");
      interrupted = true;
      return;
    }

    if (fileExists) {
      long fileLastMod = file.lastModified();
      if (fileLastMod > lastModified) {
        this.lastModified = fileLastMod;
        this.doOnChange();
        this.warnedAlready = false;
      }
    } else if (!this.warnedAlready) {
      LOGGER.warn("Configuration for file exporter was not found. It was possibly removed.");
      this.warnedAlready = true;
    }
  }

  public void run() {
    while (!this.interrupted) {
      try {
        checkAndConfigure();
        sleep(delay);
      } catch (InterruptedException var2) {
      }
    }
  }

  private void doOnChange() {
    doOnChange.run();
  }
}
