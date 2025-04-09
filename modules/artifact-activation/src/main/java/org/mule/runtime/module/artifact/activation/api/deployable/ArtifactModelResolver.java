/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

/**
 * Provides a way to obtain a {@link MuleDeployableModel} serialized in a JSON file.
 *
 * @param <M> the concrete type of model (application or model) to resolve.
 *
 * @since 4.5
 */
public interface ArtifactModelResolver<M extends MuleDeployableModel> {

  /**
   * @return a default resolver for applications.
   */
  static ArtifactModelResolver<MuleApplicationModel> applicationModelResolver() {
    return new JsonDeserializingArtifactModelResolver<>(new MuleApplicationModelJsonSerializer());
  }

  /**
   * @return a default resolver for domains.
   */
  static ArtifactModelResolver<MuleDomainModel> domainModelResolver() {
    return new JsonDeserializingArtifactModelResolver<>(new MuleDomainModelJsonSerializer());
  }

  /**
   * Loads and deserializes a {@code mule-artifact.json} file.
   *
   * @param artifactLocation the folder containing the {@code mule-artifact.json} file to deserialize.
   * @return the deserialized {@link MuleDeployableModel}.
   */
  M resolve(File artifactLocation);
}
