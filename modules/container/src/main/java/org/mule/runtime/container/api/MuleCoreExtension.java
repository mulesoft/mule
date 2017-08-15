/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
}
