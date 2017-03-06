/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
