/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.deployable;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.api.deployment.persistence.MuleApplicationModelJsonSerializer;
import org.mule.runtime.api.deployment.persistence.MuleDomainModelJsonSerializer;
import org.mule.runtime.module.artifact.activation.internal.deployable.JsonDeserializingArtifactModelResolver;

import java.io.File;

public interface ArtifactModelResolver<M extends MuleDeployableModel> {

  public static ArtifactModelResolver<MuleApplicationModel> applicationModelResolver() {
    return new JsonDeserializingArtifactModelResolver<>(new MuleApplicationModelJsonSerializer());
  }

  public static ArtifactModelResolver<MuleDomainModel> domainModelResolver() {
    return new JsonDeserializingArtifactModelResolver<>(new MuleDomainModelJsonSerializer());
  }

  M resolve(File artifactLocation);
}
