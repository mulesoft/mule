/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.exception;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.exception.MuleException.isVerboseExceptions;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

import java.util.List;

/**
 * Extends {@link ClassNotFoundException}, composing the individual exceptions of each place where the class was looked for and
 * wasn't found.
 */
@NoInstantiate
public final class CompositeClassNotFoundException extends ClassNotFoundException {

  private static final long serialVersionUID = -6941980241656380056L;

  private final String className;
  private final LookupStrategy lookupStrategy;
  private final List<ClassNotFoundException> exceptions;
  private final LazyValue<String> message;

  /**
   * Builds the exception.
   * 
   * @param className the name of the class that was trying to be loaded.
   * @param lookupStrategy the lookupStrategy that was used to load the class.
   * @param exceptions the exceptions thrown by each individual classloader that was used for the loading.
   */
  public CompositeClassNotFoundException(String className, LookupStrategy lookupStrategy,
                                         List<ClassNotFoundException> exceptions) {
    super(null, exceptions.get(0));
    message = new LazyValue<>(() -> format("Cannot load class '%s': %s", className,
                                           exceptions.stream()
                                               .map((e) -> lineSeparator() + "\t" + e.getMessage())
                                               .collect(toList())));
    this.className = className;
    this.lookupStrategy = lookupStrategy;
    this.exceptions = unmodifiableList(exceptions);
  }

  /**
   * @return the name of the class that was trying to be loaded
   */
  public String getClassName() {
    return className;
  }

  /**
   * @return the lookupStrategy that was used to load the class.
   */
  public LookupStrategy getLookupStrategy() {
    return lookupStrategy;
  }

  /**
   * @return the exceptions thrown by each individual classloader that was used for the loading.
   */
  public List<ClassNotFoundException> getExceptions() {
    return exceptions;
  }

  @Override
  public String getMessage() {
    return message.get();
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    if (isVerboseExceptions()) {
      return super.fillInStackTrace();
    } else {
      return this;
    }
  }
}
