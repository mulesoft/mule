/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
