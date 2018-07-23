/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.REPOSITORY_FOLDER;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
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
import java.util.LinkedList;
import java.util.List;

/**
 * Creates Service files.
 */
public class ServiceFileBuilder extends AbstractArtifactFileBuilder<ServiceFileBuilder> {

  private String serviceProviderClassName;
  private String[] satisfiedServiceClassNames = null;
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

  public ServiceFileBuilder satisfyingServiceClassNames(String... satisfiedServiceClassNames) {
    checkImmutable();
    checkArgument(!isEmpty(satisfiedServiceClassNames), "Property value cannot be null or empty");
    this.satisfiedServiceClassNames = satisfiedServiceClassNames;

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
    MuleServiceModelBuilder serviceModelBuilder = new MuleServiceModelBuilder();
    serviceModelBuilder.setName(getArtifactId()).setMinMuleVersion("4.0.0").setRequiredProduct(MULE);
    serviceModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    serviceModelBuilder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    serviceModelBuilder.withServiceProviderClassName(serviceProviderClassName);
    serviceModelBuilder.satisfyingServiceClassNames(satisfiedServiceClassNames);
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
