/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.plugin;

import static org.mule.runtime.core.internal.util.jar.JarLoadingUtils.loadFileContentFrom;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor.MULE_ARTIFACT_PATH_INSIDE_JAR;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Resolves the {@link MulePluginModel} by deserializing it from the {@code mule-artifact.json} within the jar of a plugin.
 *
 * @since 4.5
 */
public interface PluginModelResolver {

  /**
   * @return the default implementation of a {@link PluginModelResolver}.
   */
  static PluginModelResolver pluginModelResolver() {
    return bundleDependency -> {
      File pluginJarFile = new File(bundleDependency.getBundleUri());
      String mulePluginJsonPathInsideJarFile = MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR;

      Optional<byte[]> jsonDescriptorContentOptional;
      try {
        jsonDescriptorContentOptional = loadFileContentFrom(pluginJarFile, mulePluginJsonPathInsideJarFile);
      } catch (IOException e) {
        throw new ArtifactDescriptorCreateException(e);
      }

      return jsonDescriptorContentOptional
          .map(jsonDescriptorContent -> new MulePluginModelJsonSerializer().deserialize(new String(jsonDescriptorContent, UTF_8)))
          .orElseThrow(() -> new ArtifactDescriptorCreateException(format("The plugin descriptor '%s' on plugin file '%s' is not present",
                                                                          mulePluginJsonPathInsideJarFile, pluginJarFile)));
    };
  }

  /**
   * @param bundleDependency the bundle dependency of the plugin to get the artifact descriptor for.
   *
   * @throws ArtifactDescriptorCreateException if the serialized descriptor could not be read.
   */
  MulePluginModel resolve(BundleDependency bundleDependency) throws ArtifactDescriptorCreateException;

}
