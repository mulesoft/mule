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
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_JSON;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionModelLoader.SOAP_LOADER_ID;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel.MulePluginModelBuilder;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
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
    builder.setName(extensionModel.getName())
        .setMinMuleVersion(extensionModel.getMinMuleVersion().toCompleteNumericVersion());
    builder.withClassLoaderModelDescriber()
        .setId(MAVEN)
        .addProperty(EXPORTED_PACKAGES, exportCollector.getExportedPackages())
        .addProperty(EXPORTED_RESOURCES, exportCollector.getExportedResources());
    builder.withExtensionModelDescriber()
        .setId(getLoaderId(extensionModel))
        .addProperty(TYPE_PROPERTY_NAME, typeProperty.get().getType().getName())
        //TODO(fernandezlautaro): MULE-11136 remove this property when the manifest is dropped as generating an extension model should not require version
        .addProperty("version", extensionModel.getVersion());

    builder.withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MAVEN, emptyMap()));
    final String descriptorJson = new MulePluginModelJsonSerializer().serialize(builder.build());
    return of(new GeneratedResource(MULE_PLUGIN_JSON, descriptorJson.getBytes()));
  }

  private String getLoaderId(ExtensionModel extensionModel) {
    Optional<SoapExtensionModelProperty> soapModelProperty = extensionModel.getModelProperty(SoapExtensionModelProperty.class);
    return soapModelProperty.isPresent() ? SOAP_LOADER_ID : JAVA_LOADER_ID;
  }
}
