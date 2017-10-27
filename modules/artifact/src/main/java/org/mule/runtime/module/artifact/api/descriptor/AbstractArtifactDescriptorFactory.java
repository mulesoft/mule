/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.mule.runtime.api.deployment.meta.Product.getProductByName;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.MuleVersion.NO_REVISION;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.config.MuleManifest.getProductName;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import org.mule.runtime.api.deployment.meta.AbstractMuleArtifactModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.deployment.persistence.AbstractMuleArtifactModelJsonSerializer;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * Base class to create artifact descriptors
 *
 * @param <M> type of the artifact model that owns the descriptor
 * @param <T> type of descriptor being created
 *
 * @since 4.0
 */
public abstract class AbstractArtifactDescriptorFactory<M extends AbstractMuleArtifactModel, T extends ArtifactDescriptor>
    implements ArtifactDescriptorFactory<T> {

  public static final String ARTIFACT_DESCRIPTOR_DOES_NOT_EXISTS_ERROR = "Artifact descriptor does not exists: ";
  protected final DescriptorLoaderRepository descriptorLoaderRepository;

  /**
   * Creates a new factory
   *
   * @param descriptorLoaderRepository contains all the {@link ClassLoaderModelLoader} registered on the container. Non null
   */
  public AbstractArtifactDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository) {
    checkArgument(descriptorLoaderRepository != null, "descriptorLoaderRepository cannot be null");
    this.descriptorLoaderRepository = descriptorLoaderRepository;
  }

  @Override
  public T create(File artifactFolder, Optional<Properties> deploymentProperties) throws ArtifactDescriptorCreateException {
    final File artifactJsonFile = new File(artifactFolder, MULE_ARTIFACT_FOLDER + separator + getDescriptorFileName());
    if (!artifactJsonFile.exists()) {
      throw new ArtifactDescriptorCreateException(ARTIFACT_DESCRIPTOR_DOES_NOT_EXISTS_ERROR + artifactJsonFile);
    }

    T artifactDescriptor =
        loadFromJsonDescriptor(artifactFolder, loadModelFromJson(getDescriptorContent(artifactJsonFile)), deploymentProperties);

    return artifactDescriptor;
  }

  /**
   * @return the type of artifact begin created
   */
  protected abstract ArtifactType getArtifactType();

  /**
   * Loads a descriptor from an artifact model
   *
   * @param artifactLocation folder where the artifact is located, it can be a folder or file depending on the artifact type.
   * @param artifactModel model representing the artifact.
   * @return a descriptor matching the provided model.
   */
  protected final T loadFromJsonDescriptor(File artifactLocation, M artifactModel, Optional<Properties> deploymentProperties) {

    artifactModel.validateModel(artifactLocation.getName());

    final T descriptor = createArtifactDescriptor(artifactLocation, artifactModel.getName(), deploymentProperties);
    if (artifactLocation.isDirectory()) {
      descriptor.setRootFolder(artifactLocation);
    }
    descriptor.setBundleDescriptor(getBundleDescriptor(artifactLocation, artifactModel));
    descriptor.setMinMuleVersion(new MuleVersion(artifactModel.getMinMuleVersion()));
    descriptor.setRequiredProduct(artifactModel.getRequiredProduct());

    ClassLoaderModel classLoaderModel =
        getClassLoaderModel(artifactLocation, artifactModel.getClassLoaderModelLoaderDescriptor());
    descriptor.setClassLoaderModel(classLoaderModel);

    doDescriptorConfig(artifactModel, descriptor, artifactLocation);

    validate(descriptor);
    return descriptor;
  }

  private void validate(T descriptor) {
    MuleVersion minMuleVersion = descriptor.getMinMuleVersion();
    MuleVersion runtimeVersion = new MuleVersion(getProductVersion());
    runtimeVersion = new MuleVersion(runtimeVersion.toCompleteNumericVersion().replace("-" + runtimeVersion.getSuffix(), ""));
    if (runtimeVersion.priorTo(minMuleVersion)) {
      throw new MuleRuntimeException(
                                     createStaticMessage("Artifact %s requires a newest runtime version. Artifact required version is %s and Mule Runtime version is %s",
                                                         descriptor.getName(),
                                                         descriptor.getMinMuleVersion().toCompleteNumericVersion(),
                                                         runtimeVersion.toCompleteNumericVersion()));
    }
    Product requiredProduct = descriptor.getRequiredProduct();
    Product runtimeProduct = getProductByName(getProductName());
    if (!runtimeProduct.supports(requiredProduct)) {
      throw new MuleRuntimeException(createStaticMessage("The artifact %s requires a different runtime. The artifact required runtime is %s and the runtime is %s",
                                                         descriptor.getName(), descriptor.getRequiredProduct().name(),
                                                         runtimeProduct.name()));
    }
    validateVersion(descriptor);
  }

  protected void validateVersion(T descriptor) {
    String bundleDescriptorVersion = descriptor.getBundleDescriptor().getVersion();
    checkState(bundleDescriptorVersion != null,
               format("No version specified in the bundle descriptor of the artifact %s", descriptor.getName()));
    MuleVersion artifactVersion = new MuleVersion(bundleDescriptorVersion);
    checkState(artifactVersion.getRevision() != NO_REVISION,
               format("Artifact %s version %s must contain a revision number. The version format must be x.y.z and the z part is missing",
                      descriptor.getName(), artifactVersion));
  }

  /**
   * Generates an artifact model from a given JSON descriptor
   *
   * @param jsonString artifact descriptor in JSON format
   * @return the artifact model matching the provided JSON content.
   */
  protected M loadModelFromJson(String jsonString) {
    try {
      return deserializeArtifactModel(jsonString);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot deserialize artifact descriptor from: " + jsonString);
    }
  }

  /**
   * @return the serializer for the artifact model
   */
  protected abstract AbstractMuleArtifactModelJsonSerializer<M> getMuleArtifactModelJsonSerializer();

  /**
   * Allows subclasses to customize descriptor based on the provided model
   *
   * @param artifactModel artifact model created from the JSON descriptor
   * @param descriptor descriptor created from the model and configured with common attributes
   * @param artifactLocation folder where the artifact is located, it can be a folder or file depending on the artifact type.
   */
  protected abstract void doDescriptorConfig(M artifactModel, T descriptor, File artifactLocation);

  /**
   * @param artifactLocation folder where the artifact is located, it can be a folder or file depending on the artifact type.
   * @param name name for the created artifact
   * @param deploymentProperties properties provided for the deployment process.
   * @return a new descriptor of the type required by the factory.
   */
  protected abstract T createArtifactDescriptor(File artifactLocation, String name, Optional<Properties> deploymentProperties);

  private String getDescriptorFileName() {
    return MULE_ARTIFACT_JSON_DESCRIPTOR;
  }

  private String getDescriptorContent(File jsonFile) {
    try (InputStream stream = new FileInputStream(jsonFile)) {
      return IOUtils.toString(stream);
    } catch (IOException e) {
      throw new IllegalArgumentException(format("Could not read extension describer on artifact '%s'",
                                                jsonFile.getAbsolutePath()),
                                         e);
    }
  }

  private ClassLoaderModel getClassLoaderModel(File artifactFolder,
                                               MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor) {
    ClassLoaderModelLoader classLoaderModelLoader;
    try {
      classLoaderModelLoader =
          descriptorLoaderRepository.get(classLoaderModelLoaderDescriptor.getId(), getArtifactType(),
                                         ClassLoaderModelLoader.class);
    } catch (LoaderNotFoundException e) {
      throw new ArtifactDescriptorCreateException(invalidClassLoaderModelIdError(artifactFolder,
                                                                                 classLoaderModelLoaderDescriptor));
    }

    final ClassLoaderModel classLoaderModel;
    try {
      classLoaderModel = classLoaderModelLoader.load(artifactFolder, classLoaderModelLoaderDescriptor.getAttributes(),
                                                     getArtifactType());
    } catch (InvalidDescriptorLoaderException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
    return classLoaderModel;
  }

  private BundleDescriptor getBundleDescriptor(File appFolder, M artifactModel) {
    BundleDescriptorLoader bundleDescriptorLoader;
    try {
      bundleDescriptorLoader =
          descriptorLoaderRepository.get(artifactModel.getBundleDescriptorLoader().getId(), getArtifactType(),
                                         BundleDescriptorLoader.class);
    } catch (LoaderNotFoundException e) {
      throw new ArtifactDescriptorCreateException(invalidBundleDescriptorLoaderIdError(appFolder, artifactModel
          .getBundleDescriptorLoader()));
    }

    try {
      return bundleDescriptorLoader.load(appFolder, artifactModel.getBundleDescriptorLoader().getAttributes(), getArtifactType());
    } catch (InvalidDescriptorLoaderException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
  }

  private M deserializeArtifactModel(String jsonString) throws IOException {
    return getMuleArtifactModelJsonSerializer().deserialize(jsonString);
  }

  public static String invalidClassLoaderModelIdError(File artifactFolder,
                                                      MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor) {
    return format("The identifier '%s' for a class loader model descriptor is not supported (error found while reading artifact '%s')",
                  classLoaderModelLoaderDescriptor.getId(),
                  artifactFolder.getAbsolutePath());

  }

  public static String invalidBundleDescriptorLoaderIdError(File artifactFolder,
                                                            MuleArtifactLoaderDescriptor bundleDescriptorLoader) {
    return format("The identifier '%s' for a bundle descriptor loader is not supported (error found while reading artifact '%s')",
                  bundleDescriptorLoader.getId(),
                  artifactFolder.getAbsolutePath());
  }
}
