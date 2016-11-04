/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.mule.runtime.deployment.model.internal.plugin.descriptor.PropertiesClassloaderDescriptor.PLUGINPROPERTIES;
import static org.mule.runtime.deployment.model.internal.plugin.loader.AbstractPluginDescriptorLoader.MULE_PLUGIN_JSON_FILENAME;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;

import com.google.gson.Gson;

import java.util.Optional;

/**
 * A {@link GeneratedResourceFactory} which generates a descriptor file and stores it in {@code JSON} format
 *
 * @since 4.0
 */
public final class MulePluginJsonDescriptorGenerator implements GeneratedResourceFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<GeneratedResource> generateResource(ExtensionModel extensionModel) {
    Optional<ImplementingTypeModelProperty> typeProperty = extensionModel.getModelProperty(ImplementingTypeModelProperty.class);

    if (!typeProperty.isPresent()) {
      return Optional.empty();
    }

    MulePluginJsonFileBuilder.ClassloaderDescriptor classloaderDescriptor =
        new MulePluginJsonFileBuilder.ClassloaderDescriptor(PLUGINPROPERTIES);
    MulePluginJsonFileBuilder mulePluginJsonFileBuilder =
        new MulePluginJsonFileBuilder(extensionModel.getName(), extensionModel.getVersion(), classloaderDescriptor);
    String jsonContent = new Gson().toJson(mulePluginJsonFileBuilder);

    return Optional.of(new GeneratedResource(MULE_PLUGIN_JSON_FILENAME, jsonContent.getBytes()));
  }

  private static final class MulePluginJsonFileBuilder {

    //TODO MULE-10875 this class should be provided in mule-module-artifact, as well as a builder for it so it can be serialized in one place
    public final String name;
    public final String minMuleVersion;
    public final ClassloaderDescriptor classloaderDescriptor;

    public MulePluginJsonFileBuilder(String name, String minMuleVersion, ClassloaderDescriptor classloaderDescriptor) {
      this.name = name;
      this.minMuleVersion = minMuleVersion;
      this.classloaderDescriptor = classloaderDescriptor;
    }

    private static final class ClassloaderDescriptor {

      public final String id;

      public ClassloaderDescriptor(String id) {
        this.id = id;
      }
    }
  }
}
