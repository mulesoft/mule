/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.api;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.boot.ExtensionLoaderUtils.getLoaderById;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MulePluginBasedLoaderFinder {

  static final String META_INF_MULE_PLUGIN = "META-INF/mule-artifact.json";

  private static final MulePluginModelJsonSerializer mulePluginSerializer = new MulePluginModelJsonSerializer();
  private final MulePluginModel mulePlugin;

  MulePluginBasedLoaderFinder(InputStream json) {
    try {
      this.mulePlugin = mulePluginSerializer.deserialize(IOUtils.toString(json));
    } finally {
      closeQuietly(json);
    }
  }

  MulePluginBasedLoaderFinder(File json) throws FileNotFoundException {
    this(new FileInputStream(json));
  }

  public Map<String, Object> getParams() {
    Map<String, Object> params = new HashMap<>();
    MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor = mulePlugin.getExtensionModelLoaderDescriptor().get();
    Map<String, Object> attributes = muleArtifactLoaderDescriptor.getAttributes();
    attributes.entrySet().stream()
        .forEach(entry -> {
          params.put(entry.getKey(), entry.getValue());
        });
    return params;
  }

  public boolean isExtensionModelLoaderDescriptorDefined() {
    return mulePlugin.getExtensionModelLoaderDescriptor().isPresent();
  }

  public ExtensionModelLoader getLoader() {
    return getLoaderById(mulePlugin.getExtensionModelLoaderDescriptor().get().getId());
  }

}
