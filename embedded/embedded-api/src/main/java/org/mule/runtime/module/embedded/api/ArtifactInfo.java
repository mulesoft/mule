/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.api;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * This class describes the artifact assets to execute it within the embedded container.
 * 
 * @since 1.0
 */
public class ArtifactInfo implements Serializable {

  private final List<URI> configs;
  private final URL classesFolder;
  private final URL pomFile;
  private final URL descriptorFile;
  private final boolean enableArtifactTestDependencies;
  private final Map<String, String> artifactProperties;

  /**
   * Creates a new instance.
   * 
   * @param configs the list of configuration files to use for this artifact.
   * @param classesFolder the classes folder of the artifact
   * @param pomFile the pom file of the artifact
   * @param descriptorFile the descriptor location of the artifact
   * @param artifactProperties the artifact configuration properties
   * @param enableArtifactTestDependencies if true, it adds the test dependencies of the artifact into the artifact classpath.
   *        This is useful when using configuration files for testing that may make use of testing libraries.
   */
  public ArtifactInfo(List<URI> configs, URL classesFolder, URL pomFile, URL descriptorFile,
                      Map<String, String> artifactProperties, boolean enableArtifactTestDependencies) {
    this.configs = configs;
    this.classesFolder = classesFolder;
    this.pomFile = pomFile;
    this.descriptorFile = descriptorFile;
    this.artifactProperties = artifactProperties;
    this.enableArtifactTestDependencies = enableArtifactTestDependencies;
  }

  /**
   * @return true if the test dependencies must be used as part the artifact class loader.
   */
  public boolean isEnableArtifactTestDependencies() {
    return enableArtifactTestDependencies;
  }

  /**
   * @return the configuration properties for the artifact
   */
  public Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }

  /**
   * @return the list of configuration files for the artifact
   */
  public List<URI> getConfigs() {
    return configs;
  }

  /**
   * @return the classes folder of the artifact
   */
  public URL getClassesFolder() {
    return classesFolder;
  }

  /**
   * @return the pom file of the artifact
   */
  public URL getPomFile() {
    return pomFile;
  }

  /**
   * @return the descriptor for of the artifact
   */
  public URL getDescriptorFile() {
    return descriptorFile;
  }
}
