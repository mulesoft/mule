/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.internal.artifact;

import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

/**
 * Finds the first {@link ClassLoader} in an {@link CompositeClassLoader} that should be used when loading classes and resources
 *
 * @since 4.4, 4.3.1, 4.2.3
 */
public class CompositeClassLoaderArtifactFinder {

  private CompositeClassLoaderArtifactFinder() {
    // Private constructor to hide the implicit public one.
  }

  public static ClassLoader findClassLoader(CompositeClassLoader compositeClassLoader) {
    // Obtains the first artifact class loader that is not a plugin.
    for (ClassLoader delegate : compositeClassLoader.getDelegates()) {
      if (delegate instanceof ArtifactClassLoader && !isPluginClassLoader(delegate)) {
        return delegate;
      }
    }
    return compositeClassLoader.getDelegates().get(0);
  }

  private static boolean isPluginClassLoader(ClassLoader loggerClassLoader) {
    return ((ArtifactClassLoader) loggerClassLoader).getArtifactDescriptor() instanceof ArtifactPluginDescriptor;
  }

}
