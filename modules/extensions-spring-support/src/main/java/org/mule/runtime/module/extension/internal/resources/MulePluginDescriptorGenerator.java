/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.api.deployment.meta.Product.MULE_EE;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.PRIVILEGED_ARTIFACTS_IDS;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.PRIVILEGED_EXPORTED_PACKAGES;
import static org.mule.runtime.module.extension.api.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_RESOURCES;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;
import static java.nio.file.Paths.get;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel.MulePluginModelBuilder;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.resources.manifest.DefaultClassPackageFinder;
import org.mule.runtime.module.extension.internal.resources.manifest.ExportedPackagesCollector;
import org.mule.runtime.module.extension.internal.resources.manifest.ProcessingEnvironmentClassPackageFinder;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * A {@link GeneratedResourceFactory} which generates a {@link MulePluginModel} and stores it in {@code JSON} format
 *
 * @since 4.0
 */
// TODO: MULE-12295. This was moved here just to make it work with soap extensions.
public class MulePluginDescriptorGenerator implements GeneratedResourceFactory, ProcessingEnvironmentAware {

  private static final String AUTO_GENERATED_MULE_ARTIFACT_DESCRIPTOR = "auto-generated-" + MULE_ARTIFACT_JSON_DESCRIPTOR;
  private ProcessingEnvironment processingEnvironment;

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    final Optional<ExtensionTypeDescriptorModelProperty> typeProperty =
        extensionModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
    if (!typeProperty.isPresent()) {
      return empty();
    }

    DefaultClassPackageFinder defaultClassPackageFinder = new DefaultClassPackageFinder();
    if (processingEnvironment != null) {
      defaultClassPackageFinder.addAdditionalPackageFinder(new ProcessingEnvironmentClassPackageFinder(processingEnvironment));
    }

    final ExportedPackagesCollector exportCollector =
        new ExportedPackagesCollector(extensionModel, defaultClassPackageFinder);
    final MulePluginModelBuilder builder = new MulePluginModelBuilder();
    // Set only for testing purposes, the value will be reset by the plugin packager.
    builder.setName(extensionModel.getName());
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .setId(MULE_LOADER_ID)
        .addProperty(EXPORTED_PACKAGES, exportCollector.getExportedPackages())
        .addProperty(EXPORTED_RESOURCES, exportCollector.getExportedResources())
        .addProperty(PRIVILEGED_EXPORTED_PACKAGES, exportCollector.getPrivilegedExportedPackages())
        .addProperty(PRIVILEGED_ARTIFACTS_IDS, exportCollector.getPrivilegedArtifacts()).build());
    builder.withExtensionModelDescriber()
        .setId(getLoaderId(extensionModel))
        .addProperty(TYPE_PROPERTY_NAME, typeProperty.get().getType().getTypeName())
        .addProperty("version", extensionModel.getVersion());

    builder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));

    final Optional<LicenseModelProperty> licenseModelPropertyOptional =
        extensionModel.getModelProperty(LicenseModelProperty.class);
    builder.setRequiredProduct(extensionModel.getCategory().equals(COMMUNITY) ? MULE : MULE_EE);
    licenseModelPropertyOptional.ifPresent(licenseModelProperty -> {
      builder.setRequiredProduct(licenseModelProperty.requiresEeLicense() ? MULE_EE : MULE);
      if (licenseModelProperty.requiresEeLicense()) {
        builder.withLicenseModel().setAllowsEvaluationLicense(licenseModelProperty.isAllowsEvaluationLicense());
        licenseModelProperty.getRequiredEntitlement().ifPresent(requiredEntitlement -> builder.withLicenseModel()
            .setProvider(extensionModel.getVendor())
            .setRequiredEntitlement(requiredEntitlement));
      }
    });

    setMetadataFromSource(builder);

    final String descriptorJson = new MulePluginModelJsonSerializer().serialize(builder.build());
    return of(new GeneratedResource(false, AUTO_GENERATED_MULE_ARTIFACT_DESCRIPTOR, descriptorJson.getBytes()));
  }

  /**
   * Prevent the generation of the mule-artifact.json to override any value set explicitly by the plugin developer
   *
   * @param builder
   */
  private void setMetadataFromSource(final MulePluginModelBuilder builder) {
    readPluginModelFromSource().ifPresent(sourcePluginModel -> {
      if (sourcePluginModel.getMinMuleVersion() != null) {
        builder.setMinMuleVersion(sourcePluginModel.getMinMuleVersion());
      }
      if (sourcePluginModel.getClassLoaderModelLoaderDescriptor() != null) {
        builder.withClassLoaderModelDescriptorLoader(sourcePluginModel.getClassLoaderModelLoaderDescriptor());
      }
    });
  }

  private Optional<MulePluginModel> readPluginModelFromSource() {
    if (processingEnvironment == null) {
      return empty();
    }

    try {
      final String extensionResourcesLocation = processingEnvironment.getOptions().get(EXTENSION_RESOURCES);
      final var path = get(extensionResourcesLocation, "META-INF", "mule-artifact", MULE_ARTIFACT_JSON_DESCRIPTOR);
      if (!path.toFile().exists()) {
        return empty();
      }
      var sourceMuleArtifactJson = readString(path, UTF_8);
      return of(new MulePluginModelJsonSerializer().deserialize(sourceMuleArtifactJson));
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private String getLoaderId(ExtensionModel extensionModel) {
    return JAVA_LOADER_ID;
  }

  @Override
  public void setProcessingEnvironment(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }
}
