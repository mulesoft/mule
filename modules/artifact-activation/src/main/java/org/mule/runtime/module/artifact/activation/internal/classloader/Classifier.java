/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;

/**
 * Types of projects supported.
 */
public enum Classifier {

  MULE_APPLICATION, MULE_DOMAIN, MULE_PLUGIN;

  public static Classifier fromString(String name) {
    String classifierName = LOWER_HYPHEN.to(UPPER_UNDERSCORE, name);
    return valueOf(classifierName);
  }

  public boolean equals(String name) {
    if (name == null) {
      return false;
    }
    Classifier other;
    try {
      other = fromString(name);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return other != null && other.equals(this);
  }

  @Override
  public String toString() {
    return UPPER_UNDERSCORE.to(LOWER_HYPHEN, this.name());
  }
}
