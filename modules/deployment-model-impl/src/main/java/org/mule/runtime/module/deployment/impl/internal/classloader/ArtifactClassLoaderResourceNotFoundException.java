/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.classloader;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.exception.ResourceNotFoundException;

/**
 * Exception to be thrown within an artifact when a resource could not be found.
 *
 * @since 4.2
 */
public class ArtifactClassLoaderResourceNotFoundException extends ResourceNotFoundException {

  private final ClassLoaderNode classLoaderNode;

  public ArtifactClassLoaderResourceNotFoundException(I18nMessage message, ClassLoaderNode classLoaderNode) {
    super(message);
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
