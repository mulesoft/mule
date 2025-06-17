/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;

/**
 * Defines a template method for loading deployableModels for different kinds of deployable artifacts.
 *
 * @param <M> the concrete type of model to resolve
 */
public final class JsonDeserializingArtifactModelResolver<M extends MuleDeployableModel>
    implements ArtifactModelResolver<M> {

  private static final Logger LOGGER = getLogger(JsonDeserializingArtifactModelResolver.class);

  private final AbstractMuleArtifactModelJsonSerializer<M> jsonDeserializer;

  public JsonDeserializingArtifactModelResolver(AbstractMuleArtifactModelJsonSerializer<M> jsonDeserializer) {
    this.jsonDeserializer = jsonDeserializer;
  }

  @Override
  public M resolve(File artifactLocation) {
    final File artifactJsonFile = new File(artifactLocation, MULE_ARTIFACT_JSON_DESCRIPTOR);
    if (!artifactJsonFile.exists()) {
      throw new ArtifactActivationException(createStaticMessage("Couldn't find model file " + artifactJsonFile));
    }

    return loadModelFromJson(getDescriptorContent(artifactJsonFile));
  }

  private String getDescriptorContent(File jsonFile) {
    LOGGER.debug("Loading artifact descriptor from '{}'...", jsonFile.getAbsolutePath());

    try (InputStream stream = new BufferedInputStream(new FileInputStream(jsonFile))) {
      return IOUtils.toString(stream);
    } catch (IOException e) {
      throw new IllegalArgumentException(format("Could not read extension describer on artifact '%s'",
                                                jsonFile.getAbsolutePath()),
                                         e);
    }
  }

  /**
   * Generates an artifact model from a given JSON descriptor
   *
   * @param jsonString artifact descriptor in JSON format
   * @return the artifact model matching the provided JSON content.
   */
  private M loadModelFromJson(String jsonString) {
    try {
      return deserializeArtifactModel(jsonString);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot deserialize artifact descriptor from: " + jsonString);
    }
  }

  private M deserializeArtifactModel(String jsonString) throws IOException {
    return getMuleArtifactModelJsonSerializer().deserialize(jsonString);
  }

  /**
   * @return the serializer for the artifact model.
   */
  private AbstractMuleArtifactModelJsonSerializer<M> getMuleArtifactModelJsonSerializer() {
    return jsonDeserializer;
  }

}
