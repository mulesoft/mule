/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import java.io.File;

/**
 * Represents a class that was dynamically generated at runtime
 *
 * @param <T> the generic type of the generated class
 * @since 4.3.0
 */
public final class GeneratedClass<T> {

  private final Class<T> generatedClass;
  private final File byteCodeFile;

  /**
   * Creates a new instance
   *
   * @param generatedClass the {@link Class} that was generated
   * @param byteCodeFile   A file pointing to the generated class bytecode
   */
  public GeneratedClass(Class<T> generatedClass, File byteCodeFile) {
    this.generatedClass = generatedClass;
    this.byteCodeFile = byteCodeFile;
  }

  /**
   * @return the {@link Class} that was generated
   */
  public Class<T> getGeneratedClass() {
    return generatedClass;
  }

  /**
   * @return A file pointing to the generated class bytecode
   */
  public File getByteCodeFile() {
    return byteCodeFile;
  }
}
