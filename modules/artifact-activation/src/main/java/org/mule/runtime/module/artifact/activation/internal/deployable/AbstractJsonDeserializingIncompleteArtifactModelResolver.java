/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.deployment.meta.AbstractMuleArtifactModelBuilder;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.deployable.ArtifactModelResolver;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;

/**
 * Defines a template method for loading {@code deployableModels} for different kinds of deployable artifacts that might be
 * incomplete,thus completing them with default values.
 *
 * @param <M> the concrete type of model to resolve.
 * @param <B> the concrete type of builder to generate the complete model including default values.
 */
public abstract class AbstractJsonDeserializingIncompleteArtifactModelResolver<M extends MuleDeployableModel, B extends AbstractMuleArtifactModelBuilder<B, M>>
    extends JsonDeserializingArtifactModelResolver<M> {

  private static final Logger LOGGER = getLogger(AbstractJsonDeserializingIncompleteArtifactModelResolver.class);

  private final String modelConfigsDirectory;
  private final BundleDescriptor modelBundleDescriptor;
  private final List<BundleDependency> modelDependencies;
  private final List<BundleDependency> modelMuleRuntimeDependencies;
  private final List<String> modelPackages;
  private final List<String> modelResources;

  /**
   * Creates a new instance with the provided parameters.
   *
   * @param jsonDeserializer             deserializer to load the initial mule deployable model.
   * @param modelConfigsDirectory        directory containing configuration files.
   * @param modelBundleDescriptor        contains the GAV of the modeled project.
   * @param modelDependencies            dependencies of the modeled project.
   * @param modelMuleRuntimeDependencies dependencies of the modeled project that will be provided by the environment.
   * @param modelPackages                available packages containing java classes in the modeled project
   * @param modelResources               available resources in the modeled project.
   */
  public AbstractJsonDeserializingIncompleteArtifactModelResolver(AbstractMuleArtifactModelJsonSerializer<M> jsonDeserializer,
                                                                  String modelConfigsDirectory,
                                                                  BundleDescriptor modelBundleDescriptor,
                                                                  List<BundleDependency> modelDependencies,
                                                                  List<BundleDependency> modelMuleRuntimeDependencies,
                                                                  List<String> modelPackages,
                                                                  List<String> modelResources) {
    super(jsonDeserializer);
    this.modelConfigsDirectory = modelConfigsDirectory;
    this.modelBundleDescriptor = modelBundleDescriptor;
    this.modelDependencies = modelDependencies;
    this.modelMuleRuntimeDependencies = modelMuleRuntimeDependencies;
    this.modelPackages = modelPackages;
    this.modelResources = modelResources;
  }

  @Override
  public final M resolve(File artifactLocation) {
    final File artifactJsonFile = new File(artifactLocation, MULE_ARTIFACT_JSON_DESCRIPTOR);
    if (!artifactJsonFile.exists()) {
      throw new ArtifactActivationException(createStaticMessage("Couldn't find model file " + artifactJsonFile));
    }

    return getDefaultValuesMuleArtifactJsonGenerator(loadModelFromJson(getDescriptorContent(artifactJsonFile)), artifactLocation,
                                                     modelConfigsDirectory, modelBundleDescriptor, modelDependencies,
                                                     modelMuleRuntimeDependencies,
                                                     modelPackages, modelResources).generate();
  }

  protected abstract AbstractDefaultValuesMuleDeployableModelGenerator<M, B> getDefaultValuesMuleArtifactJsonGenerator(M originalMuleDeployableModel,
                                                                                                                       File artifactLocation,
                                                                                                                       String modelConfigsDirectory,
                                                                                                                       BundleDescriptor modelBundleDescriptor,
                                                                                                                       List<BundleDependency> modelDependencies,
                                                                                                                       List<BundleDependency> modelMuleRuntimeDependencies,
                                                                                                                       List<String> modelPackages,
                                                                                                                       List<String> modelResources);

}
