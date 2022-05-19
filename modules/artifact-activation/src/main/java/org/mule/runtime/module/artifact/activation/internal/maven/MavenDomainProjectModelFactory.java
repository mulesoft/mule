/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;

import java.io.File;

public class MavenDomainProjectModelFactory extends AbstractMavenDeployableProjectModelFactory<MuleDomainModel> {

  public MavenDomainProjectModelFactory(File projectFolder, MavenConfiguration mavenConfiguration) {
    super(projectFolder, mavenConfiguration);
  }

  public MavenDomainProjectModelFactory(File projectFolder) {
    super(projectFolder);
  }

  @Override
  protected AbstractMuleArtifactModelJsonSerializer<MuleDomainModel> getMuleArtifactModelJsonSerializer() {
    return new MuleDomainModelJsonSerializer();
  }

}
