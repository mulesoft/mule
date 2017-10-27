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
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.PRIVILEGED_ARTIFACTS_IDS;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.PRIVILEGED_EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleServiceModel;
import org.mule.runtime.api.deployment.persistence.MuleServiceModelJsonSerializer;

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
 * Resolves the {@link ArtifactUrlClassification} resources, exported packages and resources for services.
 *
 * @since 4.0
 */
public class ServiceResourcesResolver {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Resolves for the given {@link ArtifactUrlClassification} the resources exported.
   *
   * @param serviceUrlClassification {@link ArtifactUrlClassification} to be resolved
   * @return {@link ArtifactUrlClassification} with the resources resolved
   */
  public ArtifactUrlClassification resolveServiceResourcesFor(ArtifactUrlClassification serviceUrlClassification) {
    final Set<String> exportPackages = newHashSet();
    final Set<String> exportResources = newHashSet();
    final Set<String> privilegedExportedPackages = newHashSet();
    final Set<String> privilegedArtifacts = newHashSet();

    try (URLClassLoader classLoader = new URLClassLoader(serviceUrlClassification.getUrls().toArray(new URL[0]), null)) {
      logger.debug("Loading service '{}' descriptor", serviceUrlClassification.getName());
      URL artifactJsonUrl = classLoader.findResource(META_INF + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR);
      if (artifactJsonUrl == null) {
        artifactJsonUrl = classLoader.getResource(MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR);
        if (artifactJsonUrl == null) {
          throw new IllegalStateException(MULE_ARTIFACT_JSON_DESCRIPTOR + " couldn't be found for service: " +
              serviceUrlClassification.getName());
        }
      }

      MuleServiceModel muleServiceModel;
      try (InputStream stream = artifactJsonUrl.openStream()) {
        muleServiceModel = new MuleServiceModelJsonSerializer().deserialize(IOUtils.toString(stream));
      } catch (IOException e) {
        throw new IllegalArgumentException(format("Could not read extension describer on service '%s'", artifactJsonUrl),
                                           e);
      }

      MuleArtifactLoaderDescriptor classLoaderModelDescriptor = muleServiceModel.getClassLoaderModelLoaderDescriptor();
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

      return new ArtifactUrlClassification(serviceUrlClassification.getArtifactId(),
                                           muleServiceModel.getServiceProviderClassName(),
                                           serviceUrlClassification.getUrls());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
