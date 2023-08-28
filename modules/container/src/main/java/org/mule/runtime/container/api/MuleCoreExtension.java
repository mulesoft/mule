/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.api;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

/**
 * Allows Mule modules and transports to extend core functionality in an application-independent fashion.
 */
public interface MuleCoreExtension extends Lifecycle, NamedObject {

  /**
   * @param containerClassLoader container classloader which provides access to Mule API only. Non null
   */
  void setContainerClassLoader(ArtifactClassLoader containerClassLoader);

  /**
   * @return the priority (as an int), for the {@link MuleCoreExtension} to be loaded and executed. Override this method when
   *         wanting to ensure that an Extension is loaded before or after the rest. The lesser this number, the sooner it would
   *         be loaded.
   */
  default int priority() {
    return 1;
  }
}
