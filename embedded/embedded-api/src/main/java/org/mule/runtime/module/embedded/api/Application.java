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

/**
 * This class describes the artifact assets to execute it within the embedded container.
 * 
 * @since 4.0
 */
public class Application implements Serializable {

  private final List<URI> configs;
  private final URL classesFolder;
  private final URL pomFile;
  private final URL descriptorFile;

  /**
   * Creates a new instance.
   * 
   * @param configs the list of configuration files to use for this artifact.
   * @param classesFolder the classes folder of the artifact
   * @param pomFile the pom file of the artifact
   * @param descriptorFile the descriptor location of the artifact
   */
  public Application(List<URI> configs, URL classesFolder, URL pomFile, URL descriptorFile) {
    this.configs = configs;
    this.classesFolder = classesFolder;
    this.pomFile = pomFile;
    this.descriptorFile = descriptorFile;
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
