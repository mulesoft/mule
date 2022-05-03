/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static java.util.Collections.emptyList;

import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.module.artifact.activation.api.classloader.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.classloader.DeployableProjectModelFactory;

import java.io.File;

public class MavenDeployableProjectModelFactory implements DeployableProjectModelFactory {

  private final File projectFolder;
  private final MavenConfiguration mavenConfiguration;

  public MavenDeployableProjectModelFactory(File projectFolder, MavenConfiguration mavenConfiguration) {
    this.projectFolder = projectFolder;
    this.mavenConfiguration = mavenConfiguration;
  }

  @Override
  public DeployableProjectModel createDeployableProjectModel() {
    // TODO: implement
    return new DeployableProjectModel(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null);
  }

}
