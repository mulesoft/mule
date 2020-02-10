/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import java.io.File;

public final class GeneratedClass<T> {

  private final Class<T> generatedClass;
  private final File byteCodeFile;

  public GeneratedClass(Class<T> generatedClass, File byteCodeFile) {
    this.generatedClass = generatedClass;
    this.byteCodeFile = byteCodeFile;
  }

  public Class<T> getGeneratedClass() {
    return generatedClass;
  }

  public File getByteCodeFile() {
    return byteCodeFile;
  }
}
