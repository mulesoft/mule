/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

/**
 * Defines the multi-module folder name for an artifactId. It is useful when the convention of using the same artifactId as folder
 * name for the module. This will allow to have different names.
 * <p/>
 * Whenever the test class that is being executed with {@link org.mule.functional.junit4.runners.ArtifactClassLoaderRunner}
 * belongs to an artifact that is part of a multi-module maven project and the test is executed from IDE with the other modules
 * opened either from maven when the test goal is executed from the parent pom this mapper would be used due to those artifacts
 * are not going to be packaged as jars in the maven repository yet.
 *
 * @since 4.0
 */
// TODO: MULE-10085 - Avoid manual mapping declaration for multi-module folders to artifactId
public interface MavenMultiModuleArtifactMapping {

  /**
   * Gets a relative folder mapping defined for the artifact id
   *
   * @param artifactId to use for getting the folder in a multi-module mapping
   * @throws IllegalArgumentException if there is no folder mapped for the artifactId
   * @return the relative folder path for the given artifactId.
   */
  String getFolderName(String artifactId) throws IllegalArgumentException;

  /**
   * Gets the maven artifact Id by checking if the path ends with any of the mappings relative paths defined for each
   * artifacId->relativeFolder
   *
   * @param path the folder path where the classes of the artifact were found. Without the target/classes/.
   * @throws IllegalArgumentException if there is no folder mapped for the artifactId
   * @return the maven artifactId for a artifact path.
   */
  String getArtifactId(String path) throws IllegalArgumentException;

}
