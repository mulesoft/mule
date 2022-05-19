/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleApplicationModelJsonSerializer;

import java.io.File;

public class MavenApplicationProjectModelFactory extends AbstractMavenDeployableProjectModelFactory<MuleApplicationModel> {

  public MavenApplicationProjectModelFactory(File projectFolder, MavenConfiguration mavenConfiguration) {
    super(projectFolder, mavenConfiguration);
  }

  public MavenApplicationProjectModelFactory(File projectFolder) {
    super(projectFolder);
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MuleApplicationModel> getMuleArtifactModelJsonSerializer() {
    return new MuleApplicationModelJsonSerializer();
  }

}
