/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;

import java.util.List;

/**
 * Defines a composite classloader for applications
 */
public class CompositeApplicationClassLoader extends CompositeArtifactClassLoader implements ApplicationClassLoader {

  static {
    registerAsParallelCapable();
  }

  /**
   * Creates a new instance
   * 
   * @param appName name of the artifact owning the created instance.
   * @param parent parent class loader used to delegate the lookup process. Can be null.
   * @param artifactClassLoaders artifact classLoaders to compose. Non empty.
   * @param lookupPolicy policy used to guide the lookup process. Non null
   */
  public CompositeApplicationClassLoader(String appName, ClassLoader parent, List<ArtifactClassLoader> artifactClassLoaders,
                                         ClassLoaderLookupPolicy lookupPolicy) {
    super(appName, parent, artifactClassLoaders, lookupPolicy);
  }
}
