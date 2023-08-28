/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.probe.file;

import org.mule.tck.probe.Probe;

import java.io.File;

/**
 * Checks that a given file is not present in the file system
 */
public class FileDoesNotExists implements Probe {

  private final File target;

  public FileDoesNotExists(File target) {
    this.target = target;
  }

  public boolean isSatisfied() {
    return !target.exists();
  }

  public String describeFailure() {
    return String.format("File '%s' was not expected to exist", target.getName());
  }
}
