/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.common.watcher;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CONFIGURATION_WATCHER_DEFAULT_DELAY_PROPERTY;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * A watcher for changes in the configuration file.
 */
public class TracingConfigurationFileWatcher extends Thread {

  private static final Logger LOGGER = getLogger(TracingConfigurationFileWatcher.class);
  public final long DEFAULT_DELAY =
      Long.getLong(MULE_OPEN_TELEMETRY_EXPORTER_CONFIGURATION_WATCHER_DEFAULT_DELAY_PROPERTY, 60000l);
  private final Runnable doOnChange;
  private final File file;
  private final WatchService watchService;

  protected long delay = DEFAULT_DELAY;

  public TracingConfigurationFileWatcher(String filename, Runnable doOnChange) {
    super("TracingConfigurationFileWatcher");
    this.file = new File(filename);
    this.doOnChange = doOnChange;

    try {
      this.watchService = FileSystems.getDefault().newWatchService();
      file.toPath().getParent().register(watchService, ENTRY_MODIFY);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.setDaemon(true);
  }

  protected void checkAndConfigure() throws InterruptedException {
    WatchKey key = watchService.poll(delay, TimeUnit.MILLISECONDS);
    if (key == null) {
      return;
    }

    for (WatchEvent<?> event : key.pollEvents()) {
      if (ENTRY_MODIFY.equals(event.kind())) {
        Path changedFile = (Path) event.context();
        if (changedFile.equals(file.toPath().getFileName())) {
          this.doOnChange();
        }
      }
    }

    key.reset();
  }

  @Override
  public void run() {
    while (!interrupted()) {
      try {
        checkAndConfigure();
      } catch (InterruptedException ignored) {
        return;
      }
    }
  }

  private void doOnChange() {
    doOnChange.run();
  }
}
