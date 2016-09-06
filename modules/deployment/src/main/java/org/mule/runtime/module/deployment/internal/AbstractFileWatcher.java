/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileWatcher implements Runnable {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private Collection<File> files;
  private Map<File, Long> timestamps = new HashMap<File, Long>();

  public AbstractFileWatcher(File file) {
    this(Arrays.asList(file));
  }

  public AbstractFileWatcher(Collection<File> files) {
    this.files = files;

    for (File file : files) {
      timestamps.put(file, file.lastModified());
    }
  }

  @Override
  public final void run() {
    File latestFile = null;

    for (File file : files) {
      long originalTimestamp = timestamps.get(file);
      long currentTimestamp = file.lastModified();

      if (originalTimestamp != currentTimestamp) {
        timestamps.put(file, currentTimestamp);
        latestFile = file;
      }
    }

    if (latestFile != null) {
      try {
        onChange(latestFile);
      } catch (Throwable t) {
        logger.error(String.format("Monitor for %s threw an exception", latestFile), t);
      }
    }
  }

  protected abstract void onChange(File file);
}
