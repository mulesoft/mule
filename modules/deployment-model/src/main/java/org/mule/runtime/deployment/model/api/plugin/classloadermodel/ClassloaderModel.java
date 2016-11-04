/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.classloadermodel;

import org.mule.runtime.deployment.model.api.plugin.loader.Plugin;
import org.mule.runtime.deployment.model.api.plugin.dependency.ArtifactDependency;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

/**
 * Provides the necessary information to load a classloader for any given {@link Plugin}
 *
 * @since 4.0
 */
public interface ClassloaderModel {

  /**
   * @return the location of the /classes within the plugin's folder, {@link Optional#empty()} otherwise.
   * //TODO MULE-10875 pending to remove, it should not be responsibility of the ClassloaderModel the lookup of /classes (ideally this must be calculated on demand)
   */
  Optional<URL> getRuntimeClasses();

  /**
   * @return an array of the jars within /lib.
   * //TODO MULE-10875 pending to remove, it should not be responsibility of the ClassloaderModel the lookup of /classes (ideally this must be calculated on demand)
   */
  URL[] getRuntimeLibs();

  /**
   * @return filter's exported class packages. Non null.
   */
  Set<String> getExportedPackages();

  /**
   * @return filter's exported class resources. Non null.
   */
  Set<String> getExportedResources();

  /**
   * @return list of dependencies for the plugin. Non null.
   */
  Set<ArtifactDependency> getDependencies();
}
