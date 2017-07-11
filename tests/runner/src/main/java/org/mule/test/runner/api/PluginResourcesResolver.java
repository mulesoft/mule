/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_PATH_INSIDE_JAR;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.PRIVILEGED_ARTIFACTS_IDS;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.PRIVILEGED_EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.META_INF;
import static org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@link PluginUrlClassification} resources, exported packages and resources.
 *
 * @since 4.0
 */
public class PluginResourcesResolver {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Resolves for the given {@link PluginUrlClassification} the resources exported.
   *
   * @param pluginUrlClassification {@link PluginUrlClassification} to be resolved
   * @return {@link PluginUrlClassification} with the resources resolved
   */
  public PluginUrlClassification resolvePluginResourcesFor(PluginUrlClassification pluginUrlClassification) {
    final Set<String> exportPackages = newHashSet();
    final Set<String> exportResources = newHashSet();
    final Set<String> privilegedExportedPackages = newHashSet();
    final Set<String> privilegedArtifacts = newHashSet();

    try (URLClassLoader classLoader = new URLClassLoader(pluginUrlClassification.getUrls().toArray(new URL[0]), null)) {
      logger.debug("Loading plugin '{}' descriptor", pluginUrlClassification.getName());
      URL pluginJsonUrl = classLoader.findResource(META_INF + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR);
      if (pluginJsonUrl == null) {
        pluginJsonUrl = classLoader.getResource(MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR);
        if (pluginJsonUrl == null) {
          throw new IllegalStateException(MULE_ARTIFACT_JSON_DESCRIPTOR + " couldn't be found for plugin: " +
              pluginUrlClassification.getName());
        }
      }

      MulePluginModel mulePluginModel;
      try (InputStream stream = pluginJsonUrl.openStream()) {
        mulePluginModel = new MulePluginModelJsonSerializer().deserialize(IOUtils.toString(stream));
      } catch (IOException e) {
        throw new IllegalArgumentException(format("Could not read extension describer on plugin '%s'", pluginJsonUrl),
                                           e);
      }

      mulePluginModel.getClassLoaderModelLoaderDescriptor().ifPresent(
                                                                      classLoaderModelDescriptor -> {
                                                                        exportPackages
                                                                            .addAll((List<String>) classLoaderModelDescriptor
                                                                                .getAttributes().getOrDefault(EXPORTED_PACKAGES,
                                                                                                              new ArrayList<>()));
                                                                        exportResources
                                                                            .addAll((List<String>) classLoaderModelDescriptor
                                                                                .getAttributes().getOrDefault(EXPORTED_RESOURCES,
                                                                                                              new ArrayList<>()));

                                                                        privilegedExportedPackages
                                                                            .addAll((List<String>) classLoaderModelDescriptor
                                                                                .getAttributes()
                                                                                .getOrDefault(PRIVILEGED_EXPORTED_PACKAGES,
                                                                                              new ArrayList<>()));

                                                                        privilegedArtifacts
                                                                            .addAll((List<String>) classLoaderModelDescriptor
                                                                                .getAttributes()
                                                                                .getOrDefault(PRIVILEGED_ARTIFACTS_IDS,
                                                                                              new ArrayList<>()));
                                                                      });

      return new PluginUrlClassification(pluginUrlClassification.getName(), pluginUrlClassification.getUrls(),
                                         pluginUrlClassification.getExportClasses(),
                                         pluginUrlClassification.getPluginDependencies(), exportPackages, exportResources,
                                         privilegedExportedPackages, privilegedArtifacts);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
