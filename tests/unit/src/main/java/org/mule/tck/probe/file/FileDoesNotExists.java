/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
