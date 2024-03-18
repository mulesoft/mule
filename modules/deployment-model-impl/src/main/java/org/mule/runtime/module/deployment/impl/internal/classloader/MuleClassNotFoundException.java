/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.classloader;

/**
 * Exception to be thrown when there's is a class not found within the context of an artifact and that depends on user
 * configuration.
 *
 * @since 4.2
 */
public class MuleClassNotFoundException extends java.lang.ClassNotFoundException {

  private final ClassLoaderNode classLoaderNode;

  public MuleClassNotFoundException(String s, ClassLoaderNode classLoaderNode) {
    super(s);
    this.classLoaderNode = classLoaderNode;
  }

  public MuleClassNotFoundException(String s, Throwable ex, ClassLoaderNode classLoaderNode) {
    super(s, ex);
    this.classLoaderNode = classLoaderNode;
  }

  /**
   * @return a {@link ClassLoaderNode} which points to the
   *         {@link org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader} in context at the moment of the
   *         exception and provides information of the classloading hierarchy.
   */
  public ClassLoaderNode getClassLoaderNode() {
    return classLoaderNode;
  }
}
