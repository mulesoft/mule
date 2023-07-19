/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.api;

import static org.mule.runtime.module.service.api.artifact.ServiceClassLoaderFactoryProvider.serviceClassLoaderConfigurationLoader;
import static org.mule.runtime.module.service.api.discoverer.MuleServiceModelLoader.loadServiceModel;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleServiceModel;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.artifact.internal.util.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.service.internal.artifact.ServiceDescriptorFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@link ArtifactUrlClassification} resources, exported packages and resources for services.
 *
 * @since 4.0
 */
public class ServiceResourcesResolver {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ServiceDescriptorFactory serviceDescriptorFactory;

  public ServiceResourcesResolver(Collection<ArtifactClassificationNode> classificationNodes) {
    serviceDescriptorFactory =
        new ServiceDescriptorFactory(new ServiceRegistryDescriptorLoaderRepository(),
                                     ArtifactDescriptorValidatorBuilder.builder()) {

          // In the test runner we already have the Maven artifact of the service available,
          // no need to use a loader to get that again.
          @Override
          protected BundleDescriptor getBundleDescriptor(File serviceFolder,
                                                         MuleServiceModel artifactModel,
                                                         Optional<Properties> deploymentProperties) {
            return classificationNodes.stream()
                .filter(node -> {
                  try {
                    return node.getUrls().get(0).equals(serviceFolder.toURI().toURL());
                  } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                  }
                })
                .map(node -> new BundleDescriptor.Builder()
                    .setArtifactId(node.getArtifact().getArtifactId())
                    .setGroupId(node.getArtifact().getGroupId())
                    .setVersion(node.getArtifact().getVersion())
                    .setBaseVersion(node.getArtifact().getVersion())
                    .setType(node.getArtifact().getExtension())
                    .setClassifier(node.getArtifact().getClassifier())
                    .build())
                .findAny()
                .get();
          }

          @Override
          protected ClassLoaderConfiguration getClassLoaderConfiguration(File serviceFolder,
                                                                         Optional<Properties> deploymentProperties,
                                                                         MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor,
                                                                         BundleDescriptor bundleDescriptor) {
            try {
              return serviceClassLoaderConfigurationLoader().load(serviceFolder, emptyMap(),
                                                                  getArtifactType());
            } catch (InvalidDescriptorLoaderException e) {
              throw new IllegalArgumentException(e);
            }
          }
        };
  }

  /**
   * Resolves for the given {@link ArtifactUrlClassification} the resources exported.
   *
   * @param serviceUrlClassification {@link ArtifactUrlClassification} to be resolved
   * @return {@link ArtifactUrlClassification} with the resources resolved
   */
  public ServiceUrlClassification resolveServiceResourcesFor(ArtifactUrlClassification serviceUrlClassification) {
    try (URLClassLoader classLoader = new URLClassLoader(serviceUrlClassification.getUrls().toArray(new URL[0]), null)) {
      MuleServiceModel muleServiceModel = loadServiceModel(classLoader);

      // TODO: MULE-15471: to fix one service per artifact assumption
      return new ServiceUrlClassification(serviceDescriptorFactory
          .create(new File(serviceUrlClassification.getUrls().get(0).toURI()), empty()),
                                          serviceUrlClassification.getArtifactId(),
                                          "service/" + muleServiceModel.getName(),
                                          serviceUrlClassification.getUrls());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (ArtifactDescriptorCreateException | URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
