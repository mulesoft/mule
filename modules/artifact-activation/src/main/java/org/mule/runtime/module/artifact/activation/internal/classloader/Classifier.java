/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

/**
 * Types of projects supported.
 */
public enum Classifier {

  MULE_APPLICATION("mule-application"),

  MULE_DOMAIN("mule-domain"),

  MULE_PLUGIN("mule-plugin");

  public static Classifier fromString(String name) {
    for (Classifier classifier : Classifier.values()) {
      if (classifier.name.equals(name)) {
        return classifier;
      }
    }

    throw new IllegalArgumentException("No Classifier with name '" + name + "'." + name);
  }

  private final String name;

  private Classifier(String name) {
    this.name = name;
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
    return name;
  }
}
