/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.artifact;

import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

/**
 * Finds the first {@link ClassLoader} in an {@link CompositeClassLoader} that should be used when loading classes and resources
 *
 * @since 4.3
 */
public class CompositeClassLoaderArtifactFinder {

  public static ClassLoader findClassLoader(CompositeClassLoader compositeClassLoader) {
    ClassLoader firstClassLoader = compositeClassLoader.getDelegates().get(0);
    // Obtains the first artifact class loader that is not a plugin
    for (ClassLoader delegate : compositeClassLoader.getDelegates()) {
      if (delegate instanceof ArtifactClassLoader && !isPluginClassLoader(delegate)) {
        return delegate;
      }
    }
    return firstClassLoader;
  }

  private static boolean isPluginClassLoader(ClassLoader loggerClassLoader) {
    return ((ArtifactClassLoader) loggerClassLoader).getArtifactDescriptor() instanceof ArtifactPluginDescriptor;
  }

}
