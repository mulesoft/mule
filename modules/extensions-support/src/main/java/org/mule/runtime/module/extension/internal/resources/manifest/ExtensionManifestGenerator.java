/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.DESCRIBER_ID;
import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.TYPE_PROPERTY_NAME;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.manifest.ExtensionManifestBuilder;
import org.mule.runtime.extension.api.persistence.manifest.ExtensionManifestXmlSerializer;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;

import java.util.Optional;

/**
 * A {@link GeneratedResourceFactory} which generates a {@link ExtensionManifest} and stores it in {@code XML} format
 *
 * @since 4.0
 */
public final class ExtensionManifestGenerator implements GeneratedResourceFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    Optional<ImplementingTypeModelProperty> typeProperty = extensionModel.getModelProperty(ImplementingTypeModelProperty.class);

    if (!typeProperty.isPresent()) {
      return Optional.empty();
    }

    ExportedArtifactsCollector exportCollector = new ExportedArtifactsCollector(extensionModel);
    ExtensionManifestBuilder builder = new ExtensionManifestBuilder();
    builder.setName(extensionModel.getName()).setDescription(extensionModel.getDescription())
        .setVersion(extensionModel.getVersion()).setMinMuleVersion(extensionModel.getMinMuleVersion())
        .addExportedPackages(exportCollector.getExportedPackages()).addExportedResources(exportCollector.getExportedResources())
        .withDescriber().setId(DESCRIBER_ID).addProperty(TYPE_PROPERTY_NAME, typeProperty.get().getType().getName());

    String manifestXml = new ExtensionManifestXmlSerializer().serialize(builder.build());
    return Optional.of(new GeneratedResource(EXTENSION_MANIFEST_FILE_NAME, manifestXml.getBytes()));
  }
}
