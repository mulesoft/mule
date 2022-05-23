/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.plugin;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.internal.classloader.AbstractArtifactClassLoaderConfigurationAssembler;
import org.mule.runtime.module.artifact.activation.internal.classloader.model.ClassLoaderModelAssembler;
import org.mule.runtime.module.artifact.activation.internal.classloader.ArtifactClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Assembles the class loader configuration for a plugin.
 */
public class PluginClassLoaderConfigurationAssembler extends AbstractArtifactClassLoaderConfigurationAssembler {

  private final File artifactLocation;
  private final MulePluginModel pluginModel;
  private final List<BundleDependency> bundleDependencies;
  private final BundleDescriptor bundleDescriptor;
  private final DeployableArtifactDescriptor ownerDescriptor;

  public PluginClassLoaderConfigurationAssembler(ArtifactCoordinates artifactCoordinates, List<String> exportedPackages,
                                                 List<String> exportedResources, List<Artifact> dependencies,
                                                 File artifactLocation, MulePluginModel pluginModel,
                                                 List<BundleDependency> bundleDependencies, BundleDescriptor bundleDescriptor,
                                                 DeployableArtifactDescriptor ownerDescriptor) {
    super(new ClassLoaderModelAssembler(artifactCoordinates, dependencies, exportedPackages, exportedResources)
        .createClassLoaderModel());
    this.artifactLocation = artifactLocation;
    this.pluginModel = pluginModel;
    this.bundleDependencies = bundleDependencies;
    this.bundleDescriptor = bundleDescriptor;
    this.ownerDescriptor = ownerDescriptor;
  }

  @Override
  protected List<URL> addArtifactSpecificClassLoaderConfiguration(ArtifactClassLoaderConfigurationBuilder artifactClassLoaderConfigurationBuilder) {
    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    if (ownerDescriptor != null) {
      ownerDescriptor.getClassLoaderModel().getDependencies().stream()
          .filter(bundleDescriptor -> bundleDescriptor.getDescriptor().isPlugin())
          .filter(bundleDescriptor -> bundleDescriptor.getDescriptor().getGroupId()
              .equals(this.bundleDescriptor.getGroupId())
              && bundleDescriptor.getDescriptor().getArtifactId().equals(this.bundleDescriptor.getArtifactId()))
          .filter(bundleDependency -> bundleDependency.getAdditionalDependenciesList() != null
              && !bundleDependency.getAdditionalDependenciesList().isEmpty())
          .forEach(bundleDependency -> processPluginAdditionalDependenciesURIs(bundleDependency,
                                                                               artifactClassLoaderConfigurationBuilder)
                                                                                   .forEach(uri -> {
                                                                                     final URL dependencyArtifactUrl;
                                                                                     try {
                                                                                       dependencyArtifactUrl = uri.toURL();
                                                                                     } catch (MalformedURLException e) {
                                                                                       throw new ArtifactActivationException(createStaticMessage(format("There was an exception obtaining the URL for the artifact [%s], file [%s]",
                                                                                                                                                        artifactLocation
                                                                                                                                                            .getAbsolutePath(),
                                                                                                                                                        uri)),
                                                                                                                             e);
                                                                                     }
                                                                                     dependenciesArtifactsUrls
                                                                                         .add(dependencyArtifactUrl);
                                                                                   }));
    }

    return dependenciesArtifactsUrls;
  }

  private List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency,
                                                            ArtifactClassLoaderConfigurationBuilder artifactClassLoaderConfigurationBuilder) {
    return bundleDependency.getAdditionalDependenciesList().stream().map(additionalDependency -> {
      // TODO: check if the dependency belongs to the deny-list
      artifactClassLoaderConfigurationBuilder.withLocalPackages(additionalDependency.getPackages());
      artifactClassLoaderConfigurationBuilder.withLocalResources(additionalDependency.getResources());
      return additionalDependency.getBundleUri();
    }).collect(toList());
  }

  @Override
  protected List<BundleDependency> getBundleDependencies() {
    return bundleDependencies;
  }

  @Override
  protected Set<String> getExportedPackages() {
    return new HashSet<>(getAttribute(pluginModel.getClassLoaderModelLoaderDescriptor().getAttributes(), EXPORTED_PACKAGES));
  }

  @Override
  protected Set<String> getExportedResources() {
    return new HashSet<>(getAttribute(pluginModel.getClassLoaderModelLoaderDescriptor().getAttributes(), EXPORTED_RESOURCES));
  }

  private List<String> getAttribute(Map<String, Object> attributes, String attribute) {
    final Object attributeObject = attributes.getOrDefault(attribute, new ArrayList<String>());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

  @Override
  protected File getProjectFolder() {
    return artifactLocation;
  }

  @Override
  protected boolean shouldPopulateLocalPackages() {
    // TODO: check if it belongs to the deny-list to decide whether local packages should be populated
    return true;
  }
}
