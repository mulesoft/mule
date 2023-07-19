/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.nativelib;

public enum NativeLibraryFileExtension {

  DLL, DYLIB, JNILIB, SO;

  public String value() {
    return this.name().toLowerCase();
  }
}
