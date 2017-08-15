/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.meta.MuleVersion;

import java.io.File;
import java.nio.file.Paths;

public class ArtifactDescriptor {

  public static final String MULE_ARTIFACT_JSON_DESCRIPTOR = "mule-artifact.json";
  public static final String MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION =
      Paths.get("META-INF", "mule-artifact", MULE_ARTIFACT_JSON_DESCRIPTOR).toString();

  private final String name;
  private File rootFolder;
  private ClassLoaderModel classLoaderModel = ClassLoaderModel.NULL_CLASSLOADER_MODEL;
  private BundleDescriptor bundleDescriptor;
  private MuleVersion minMuleVersion;

  /**
   * Creates a new descriptor for a named artifact
   *
   * @param name artifact name. Non empty.
   */
  public ArtifactDescriptor(String name) {
    checkArgument(!isEmpty(name), "Artifact name cannot be empty");
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public File getRootFolder() {
    return rootFolder;
  }

  public void setRootFolder(File rootFolder) {
    if (rootFolder == null) {
      throw new IllegalArgumentException("Root folder cannot be null");
    }

    this.rootFolder = rootFolder;
  }

  /**
   * @return the minimal mule version required to run this artifact
   */
  public MuleVersion getMinMuleVersion() {
    return minMuleVersion;
  }

  /**
   * @param minMuleVersion the minimal mule version required to run this artifact
   */
  public void setMinMuleVersion(MuleVersion minMuleVersion) {
    this.minMuleVersion = minMuleVersion;
  }

  public ClassLoaderModel getClassLoaderModel() {
    return classLoaderModel;
  }

  public void setClassLoaderModel(ClassLoaderModel classLoaderModel) {
    this.classLoaderModel = classLoaderModel;
  }

  public BundleDescriptor getBundleDescriptor() {
    return bundleDescriptor;
  }

  public void setBundleDescriptor(BundleDescriptor bundleDescriptor) {
    this.bundleDescriptor = bundleDescriptor;
  }

  @Override
  public String toString() {
    return format("%s[%s]", getClass().getSimpleName(), getName());
  }
}
