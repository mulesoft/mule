/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import org.mule.runtime.api.deployment.meta.AbstractMuleArtifactModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.module.artifact.activation.internal.ExecutionEnvironment;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;

import java.io.File;

/**
 * Base class to create artifact descriptors.
 *
 * @param <M> type of the artifact model that owns the descriptor.
 * @param <T> type of descriptor being created.
 */
public abstract class AbstractArtifactDescriptorFactory<M extends AbstractMuleArtifactModel, T extends ArtifactDescriptor> {

  private final File artifactLocation;
  private M artifactModel;
  private final ArtifactDescriptorValidator artifactDescriptorValidator;

  public AbstractArtifactDescriptorFactory(File artifactLocation,
                                           ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    this.artifactLocation = artifactLocation;

    this.artifactDescriptorValidator = artifactDescriptorValidatorBuilder
        .validateMinMuleVersion()
        .validateMuleProduct()
        .validateVersionFormat()
        .validateSupportedJavaVersions()
        .build();
  }

  public T create() {
    artifactModel = createArtifactModel();
    validateModel(artifactModel);
    final T descriptor = doCreateArtifactDescriptor();

    BundleDescriptor bundleDescriptor = getBundleDescriptor();
    descriptor.setBundleDescriptor(bundleDescriptor);
    descriptor.setMinMuleVersion(new MuleVersion(artifactModel.getMinMuleVersion()));
    descriptor.setRequiredProduct(artifactModel.getRequiredProduct());

    ClassLoaderConfiguration classLoaderConfiguration =
        getClassLoaderConfiguration(artifactModel.getClassLoaderModelLoaderDescriptor());
    descriptor.setClassLoaderConfiguration(classLoaderConfiguration);

    doDescriptorConfig(descriptor);

    doValidation(descriptor);

    return descriptor;
  }

  private void validateModel(M artifactModel) {
    if (!ExecutionEnvironment.isMuleFramework()) {
      artifactModel.validateModel(this.artifactLocation.getName());
    }
  }

  protected void doValidation(T descriptor) {
    artifactDescriptorValidator.validate(descriptor);
  }

  protected M getArtifactModel() {
    return artifactModel;
  }

  public File getArtifactLocation() {
    return artifactLocation;
  }

  protected abstract M createArtifactModel();

  protected abstract void doDescriptorConfig(T descriptor);

  protected abstract ClassLoaderConfiguration getClassLoaderConfiguration(MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor);

  protected abstract BundleDescriptor getBundleDescriptor();

  protected abstract T doCreateArtifactDescriptor();
}
