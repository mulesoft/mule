/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static java.util.Collections.emptyEnumeration;

import org.mule.module.artifact.classloader.ClassLoaderResourceReleaser;

import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 * Just an empty {@link ResourceBundle}, used to test {@link ClassLoaderResourceReleaser ResourceBundle cleanup}.
 */
public class TestResourceBundle extends ResourceBundle {

  public Object handleGetObject(String key) {
    return null;
  }

  public Enumeration<String> getKeys() {
    return emptyEnumeration();
  }
}
