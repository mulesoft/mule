/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import org.mule.api.annotation.NoImplement;

import java.util.Set;

/**
 * Filter that provides access to the configured exported packages and resources.
 */
@NoImplement
public interface ArtifactClassLoaderFilter extends ClassLoaderFilter {


  /**
   * @return filter's exported class packages. Non null
   */
  Set<String> getExportedClassPackages();

  /**
   * @return filter's exported resources. Non null
   */
  Set<String> getExportedResources();
}
