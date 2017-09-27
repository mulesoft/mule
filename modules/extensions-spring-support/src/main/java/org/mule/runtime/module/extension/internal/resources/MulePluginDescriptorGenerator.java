/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.api.deployment.meta.Product.MULE_EE;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.PRIVILEGED_ARTIFACTS_IDS;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.PRIVILEGED_EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.soap.api.loader.SoapExtensionModelLoader.SOAP_LOADER_ID;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel.MulePluginModelBuilder;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;
import org.mule.runtime.module.extension.internal.resources.manifest.ExportedArtifactsCollector;
import org.mule.runtime.module.extension.soap.internal.loader.property.SoapExtensionModelProperty;

import java.util.Optional;

/**
 * A {@link GeneratedResourceFactory} which generates a {@link MulePluginModel} and stores it in {@code JSON} format
 *
 * @since 4.0
 */
// TODO: MULE-12295. This was moved here just to make it work with soap extensions.
public class MulePluginDescriptorGenerator implements GeneratedResourceFactory {

  private static final String AUTO_GENERATED_MULE_ARTIFACT_DESCRIPTOR = "auto-generated-" + MULE_ARTIFACT_JSON_DESCRIPTOR;

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    final Optional<ImplementingTypeModelProperty> typeProperty =
        extensionModel.getModelProperty(ImplementingTypeModelProperty.class);
    if (!typeProperty.isPresent()) {
      return empty();
    }

    final ExportedArtifactsCollector exportCollector = new ExportedArtifactsCollector(extensionModel);
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
        .addProperty(TYPE_PROPERTY_NAME, typeProperty.get().getType().getName())
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

    final String descriptorJson = new MulePluginModelJsonSerializer().serialize(builder.build());
    return of(new GeneratedResource(AUTO_GENERATED_MULE_ARTIFACT_DESCRIPTOR, descriptorJson.getBytes()));
  }

  private String getLoaderId(ExtensionModel extensionModel) {
    Optional<SoapExtensionModelProperty> soapModelProperty = extensionModel.getModelProperty(SoapExtensionModelProperty.class);
    return soapModelProperty.isPresent() ? SOAP_LOADER_ID : JAVA_LOADER_ID;
  }
}
