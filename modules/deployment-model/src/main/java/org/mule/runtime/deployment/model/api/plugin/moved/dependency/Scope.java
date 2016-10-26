/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.moved.dependency;

/**
 * Different types of supported scopes used to calculate the needed artifacts for any given plugin.
 *
 * @since 4.0
 * TODO MULE-10785 REMOVE THIS CLASS
 */
public enum Scope {
  /**
   * Compile dependencies are available in all classpaths of a project. Furthermore, those dependencies are propagated to dependent projects.
   */
  COMPILE,
  /**
   * This is much like compile, but indicates you expect the JDK or a container to provide the dependency at runtime.
   */
  PROVIDED,
  /**
   * This scope indicates that the dependency is not required for compilation, but is for execution.
   */
  RUNTIME,
  /**
   * his scope indicates that the dependency is not required for normal use of the application, and is only available for the test compilation and execution phases. This scope is not transitive.
   */
  TEST,
  /**
   * This scope is similar to provided except that you have to provide the JAR which contains it explicitly.
   * TODO: does makes sense to have SYSTEM scope?
   */
  SYSTEM
}
