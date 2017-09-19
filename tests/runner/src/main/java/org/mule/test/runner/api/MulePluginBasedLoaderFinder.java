/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.api;

import static org.mule.test.runner.utils.ExtensionLoaderUtils.getLoaderById;
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
    this.mulePlugin = mulePluginSerializer.deserialize(IOUtils.toString(json));
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
