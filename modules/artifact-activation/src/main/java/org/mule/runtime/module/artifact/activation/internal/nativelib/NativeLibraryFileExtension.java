/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.nativelib;

public enum NativeLibraryFileExtension {

  DLL, DYLIB, JNILIB, SO;

  public String value() {
    return this.name().toLowerCase();
  }
}
