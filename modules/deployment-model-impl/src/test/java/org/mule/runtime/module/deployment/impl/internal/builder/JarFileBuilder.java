/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.builder;

import org.mule.runtime.module.artifact.builder.AbstractDependencyFileBuilder;

import java.io.File;

/**
 * File builder to describe a regular jar dependency.
 */
public class JarFileBuilder extends AbstractDependencyFileBuilder<JarFileBuilder> {

  private final File artifactFile;

  public JarFileBuilder(String artifactId, File jarFile) {
    super(artifactId);
    this.artifactFile = jarFile;
  }

  @Override
  public File getArtifactFile() {
    return artifactFile;
  }

  @Override
  protected JarFileBuilder getThis() {
    return this;
  }

}
