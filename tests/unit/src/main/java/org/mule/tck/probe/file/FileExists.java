/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.probe.file;

import org.mule.tck.probe.Probe;

import java.io.File;

/**
 * Checks that a given file exists in the file system
 */
public class FileExists implements Probe {

  private final File target;

  public FileExists(File target) {
    this.target = target;
  }

  public boolean isSatisfied() {
    return target.exists();
  }

  public String describeFailure() {
    return String.format("File '%s' does not exists", target.getName());
  }
}
