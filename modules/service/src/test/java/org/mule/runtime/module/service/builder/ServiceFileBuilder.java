/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.REPOSITORY_FOLDER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleServiceContractModel;
import org.mule.runtime.api.deployment.meta.MuleServiceModel.MuleServiceModelBuilder;
import org.mule.runtime.api.deployment.persistence.MuleServiceModelJsonSerializer;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.runtime.module.artifact.builder.AbstractDependencyFileBuilder;
import org.mule.tck.ZipUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates Service files.
 */
public class ServiceFileBuilder extends AbstractArtifactFileBuilder<ServiceFileBuilder> {

  private String serviceProviderClassName;
  private String contractClassName = null;
  private boolean unpack = false;

  /**
   * Creates a new builder
   *
   * @param artifactId artifact identifier. Non empty.
   */
  public ServiceFileBuilder(String artifactId) {
    super(artifactId);
  }

  @Override
  protected ServiceFileBuilder getThis() {
    return this;
  }

  public ServiceFileBuilder unpack(boolean unpack) {
    this.unpack = unpack;
    return this;
  }

  /**
   * Configures the service provider
   *
   * @param className service provider class name. Non blank.
   * @return the same builder instance
   */
  public ServiceFileBuilder withServiceProviderClass(String className) {
    checkImmutable();
    checkArgument(!isBlank(className), "Property value cannot be blank");
    serviceProviderClassName = className;

    return this;
  }

  public ServiceFileBuilder forContract(String contractClassName) {
    checkImmutable();
    checkArgument(!isBlank(contractClassName), "Property value cannot be blank");
    this.contractClassName = contractClassName;

    return this;
  }

  @Override
  protected final List<ZipUtils.ZipResource> getCustomResources() {
    final List<ZipUtils.ZipResource> customResources = new LinkedList<>();

    for (AbstractDependencyFileBuilder dependencyFileBuilder : getAllCompileDependencies()) {
      customResources.add(new ZipUtils.ZipResource(dependencyFileBuilder.getArtifactFile().getAbsolutePath(),
                                                   Paths.get(REPOSITORY_FOLDER,
                                                             dependencyFileBuilder.getArtifactFileRepositoryPath())
                                                       .toString()));

      customResources.add(new ZipUtils.ZipResource(dependencyFileBuilder.getArtifactPomFile().getAbsolutePath(),
                                                   Paths.get(REPOSITORY_FOLDER,
                                                             dependencyFileBuilder.getArtifactFilePomRepositoryPath())
                                                       .toString()));
    }

    File serviceDescriptor = createServiceJsonDescriptorFile();
    customResources.add(new ZipUtils.ZipResource(serviceDescriptor.getAbsolutePath(), MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION));

    return customResources;
  }

  private File createServiceJsonDescriptorFile() {
    File serviceDescriptor = new File(getTempFolder(), getArtifactId() + "service.json");
    serviceDescriptor.deleteOnExit();
    MuleServiceModelBuilder serviceModelBuilder = new MuleServiceModelBuilder()
        .setName(getArtifactId())
        .setMinMuleVersion("4.2.0")
        .setRequiredProduct(MULE)
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
        .withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
        .withContracts(Arrays.asList(new MuleServiceContractModel(serviceProviderClassName, contractClassName)));

    String serviceDescriptorContent = new MuleServiceModelJsonSerializer().serialize(serviceModelBuilder.build());
    try (FileWriter fileWriter = new FileWriter(serviceDescriptor)) {
      fileWriter.write(serviceDescriptorContent);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    return serviceDescriptor;
  }

  @Override
  public String getConfigFile() {
    return null;
  }

  @Override
  public File getArtifactFile() {
    boolean newlyCreated = artifactFile == null;
    File file = super.getArtifactFile();
    if (unpack && newlyCreated && !file.isDirectory()) {
      File unpacked = new File(file.getParentFile(), file.getName() + "-unpack");
      unpacked.mkdirs();
      try {
        unzip(file, unpacked, false);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }

      file.delete();
      final File newFile = new File(file.getParentFile(), file.getName().replaceAll(".jar", ""));
      unpacked.renameTo(newFile);
      file = new File(newFile.getAbsolutePath());
      artifactFile = file;
    }

    return file;
  }
}
