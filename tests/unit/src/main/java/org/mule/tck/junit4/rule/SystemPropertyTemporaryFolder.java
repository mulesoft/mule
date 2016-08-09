/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.rule;

import java.io.File;

import org.junit.rules.TemporaryFolder;

/**
 * Sets up a temporary folder that is also set as a system property before a test and guaranties to tear it down afterward.
 */
public class SystemPropertyTemporaryFolder extends TemporaryFolder {

  private final String propertyName;
  private String originalPropertyValue;

  public SystemPropertyTemporaryFolder(String propertyName) {
    this.propertyName = propertyName;
  }

  public SystemPropertyTemporaryFolder(File parentFolder, String propertyName) {
    super(parentFolder);
    this.propertyName = propertyName;
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    originalPropertyValue = System.setProperty(propertyName, getRoot().getCanonicalPath());
  }

  @Override
  protected void after() {
    if (originalPropertyValue == null) {
      System.clearProperty(propertyName);
    } else {
      System.setProperty(propertyName, originalPropertyValue);
    }
    super.after();
  }
}
